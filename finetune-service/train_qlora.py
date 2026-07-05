import argparse
from pathlib import Path

import torch
from datasets import load_dataset
from peft import LoraConfig, prepare_model_for_kbit_training
from transformers import (
    AutoModelForCausalLM,
    AutoTokenizer,
    BitsAndBytesConfig,
    TrainingArguments,
)
from trl import SFTTrainer


DEFAULT_BASE_MODEL = "Qwen/Qwen2.5-1.5B-Instruct"
DEFAULT_DATA_PATH = "train_data.jsonl"
DEFAULT_OUTPUT_DIR = "uth_lora_adapter"


def build_prompt(instruction: str, input_text: str, output_text: str) -> str:
    instruction = (instruction or "").strip()
    input_text = (input_text or "").strip()
    output_text = (output_text or "").strip()

    if input_text:
        return f"""### Cau hoi:
{instruction}

### Ngu canh:
{input_text}

### Tra loi:
{output_text}"""

    return f"""### Cau hoi:
{instruction}

### Tra loi:
{output_text}"""


def formatting_func(batch):
    instructions = batch["instruction"]
    inputs = batch.get("input", [""] * len(instructions))
    outputs = batch["output"]

    prompts = []
    for index in range(len(instructions)):
        prompts.append(build_prompt(instructions[index], inputs[index], outputs[index]))
    return prompts


def parse_args():
    parser = argparse.ArgumentParser(description="Train QLoRA adapter for the Vietnamese course chatbot.")
    parser.add_argument("--base_model", type=str, default=DEFAULT_BASE_MODEL)
    parser.add_argument("--data_path", type=str, default=DEFAULT_DATA_PATH)
    parser.add_argument("--output_dir", type=str, default=DEFAULT_OUTPUT_DIR)
    parser.add_argument("--num_train_epochs", type=int, default=3)
    parser.add_argument("--per_device_train_batch_size", type=int, default=2)
    parser.add_argument("--gradient_accumulation_steps", type=int, default=4)
    parser.add_argument("--learning_rate", type=float, default=2e-4)
    parser.add_argument("--max_seq_length", type=int, default=1024)
    parser.add_argument("--logging_steps", type=int, default=10)
    parser.add_argument("--save_strategy", type=str, default="epoch")
    parser.add_argument("--seed", type=int, default=42)
    return parser.parse_args()


def resolve_precision_arguments() -> tuple[bool, bool]:
    if torch.cuda.is_available() and torch.cuda.is_bf16_supported():
        return False, True
    return True, False


def write_ollama_modelfile(output_dir: Path, base_model: str) -> None:
    modelfile_path = output_dir / "Modelfile"
    modelfile_path.write_text(
        f"""FROM {base_model}

ADAPTER adapter_model.safetensors

SYSTEM \"\"\"
Ban la chatbot ho tro sinh vien hoi dap dua tren tai lieu mon hoc.
Chi tra loi bang tieng Viet.
Uu tien tra loi dung noi dung da hoc trong slide va tai lieu mon hoc.
Neu cau hoi nam ngoai pham vi tai lieu, hay noi rang ban khong co du thong tin.
\"\"\"

PARAMETER temperature 0.2
PARAMETER top_p 0.9
PARAMETER num_ctx 4096
""",
        encoding="utf-8",
    )


def main():
    args = parse_args()

    data_path = Path(args.data_path).resolve()
    output_dir = Path(args.output_dir).resolve()
    output_dir.mkdir(parents=True, exist_ok=True)

    if not data_path.exists():
        raise FileNotFoundError(f"Khong tim thay dataset: {data_path}")

    print("Base model:", args.base_model)
    print("Data path:", data_path)
    print("Output dir:", output_dir)

    tokenizer = AutoTokenizer.from_pretrained(
        args.base_model,
        trust_remote_code=True,
    )
    if tokenizer.pad_token is None:
        tokenizer.pad_token = tokenizer.eos_token

    quant_config = BitsAndBytesConfig(
        load_in_4bit=True,
        bnb_4bit_quant_type="nf4",
        bnb_4bit_use_double_quant=True,
        bnb_4bit_compute_dtype=torch.bfloat16 if torch.cuda.is_available() else torch.float32,
    )

    model = AutoModelForCausalLM.from_pretrained(
        args.base_model,
        quantization_config=quant_config,
        device_map="auto",
        trust_remote_code=True,
    )
    model = prepare_model_for_kbit_training(model)

    dataset = load_dataset(
        "json",
        data_files=str(data_path),
        split="train",
    )
    if len(dataset) == 0:
        raise RuntimeError("Dataset train_data.jsonl dang rong, khong the bat dau fine-tune.")

    lora_config = LoraConfig(
        r=16,
        lora_alpha=32,
        lora_dropout=0.05,
        bias="none",
        task_type="CAUSAL_LM",
        target_modules="all-linear",
    )

    use_fp16, use_bf16 = resolve_precision_arguments()
    training_args = TrainingArguments(
        output_dir=str(output_dir),
        num_train_epochs=args.num_train_epochs,
        per_device_train_batch_size=args.per_device_train_batch_size,
        gradient_accumulation_steps=args.gradient_accumulation_steps,
        learning_rate=args.learning_rate,
        logging_steps=args.logging_steps,
        save_strategy=args.save_strategy,
        fp16=use_fp16,
        bf16=use_bf16,
        report_to="none",
        seed=args.seed,
    )

    trainer = SFTTrainer(
        model=model,
        tokenizer=tokenizer,
        train_dataset=dataset,
        formatting_func=formatting_func,
        peft_config=lora_config,
        args=training_args,
        max_seq_length=args.max_seq_length,
        packing=False,
    )

    trainer.train()
    trainer.model.save_pretrained(str(output_dir))
    tokenizer.save_pretrained(str(output_dir))
    write_ollama_modelfile(output_dir, args.base_model)

    print("Da luu LoRA adapter tai:", output_dir)
    print("Da tao Modelfile cho Ollama tai:", output_dir / "Modelfile")


if __name__ == "__main__":
    main()
