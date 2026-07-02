import argparse
import os
import torch

from datasets import load_dataset
from transformers import (
    AutoModelForCausalLM,
    AutoTokenizer,
    BitsAndBytesConfig,
    TrainingArguments
)
from peft import LoraConfig, prepare_model_for_kbit_training
from trl import SFTTrainer


# Thay thế hàm build_prompt và cấu hình trong main bằng cách gộp thành hàm xử lý batch:
def formatting_func(batch):
    output_texts = []
    # Lặp qua từng phần tử trong batch dữ liệu
    for i in range(len(batch["instruction"])):
        instruction = batch["instruction"][i]
        # Xử lý trường input/ngữ cảnh an toàn nếu có dữ liệu rỗng
        input_text = batch["input"][i] if "input" in batch and batch["input"][i] else ""
        output = batch["output"][i]

        if input_text:
            prompt = f"""### Câu hỏi:
{instruction}

### Ngữ cảnh:
{input_text}

### Trả lời:
{output}"""
        else:
            prompt = f"""### Câu hỏi:
{instruction}

### Trả lời:
{output}"""
        output_texts.append(prompt)
    return output_texts


def main():
    parser = argparse.ArgumentParser()

    parser.add_argument(
        "--base_model",
        type=str,
        default="Qwen/Qwen2.5-1.5B-Instruct"
    )

    parser.add_argument(
        "--data_path",
        type=str,
        default="train_data.jsonl"
    )

    parser.add_argument(
        "--output_dir",
        type=str,
        default="uth_lora_adapter"
    )

    args = parser.parse_args()

    print("Base model:", args.base_model)
    print("Data path:", args.data_path)
    print("Output dir:", args.output_dir)

    tokenizer = AutoTokenizer.from_pretrained(
        args.base_model,
        trust_remote_code=True
    )

    if tokenizer.pad_token is None:
        tokenizer.pad_token = tokenizer.eos_token

    quant_config = BitsAndBytesConfig(
        load_in_4bit=True,
        bnb_4bit_quant_type="nf4",
        bnb_4bit_use_double_quant=True,
        bnb_4bit_compute_dtype=torch.bfloat16
    )

    model = AutoModelForCausalLM.from_pretrained(
        args.base_model,
        quantization_config=quant_config,
        device_map="auto",
        trust_remote_code=True
    )

    model = prepare_model_for_kbit_training(model)

    dataset = load_dataset(
        "json",
        data_files=args.data_path,
        split="train"
    )

    def formatting_func(example):
        return build_prompt(example)

    lora_config = LoraConfig(
        r=16,
        lora_alpha=32,
        lora_dropout=0.05,
        bias="none",
        task_type="CAUSAL_LM",
        target_modules="all-linear"
    )

    training_args = TrainingArguments(
        output_dir=args.output_dir,
        num_train_epochs=3,
        per_device_train_batch_size=2,
        gradient_accumulation_steps=4,
        learning_rate=2e-4,
        logging_steps=10,
        save_strategy="epoch",
        fp16=True,
        report_to="none"
    )

    trainer = SFTTrainer(
        model=model,
        tokenizer=tokenizer,
        train_dataset=dataset,
        formatting_func=formatting_func,
        peft_config=lora_config,
        args=training_args,
        max_seq_length=1024,
        packing=False
    )

    trainer.train()

    trainer.model.save_pretrained(args.output_dir)
    tokenizer.save_pretrained(args.output_dir)

    print("Đã lưu LoRA adapter tại:", args.output_dir)


if __name__ == "__main__":
    main()