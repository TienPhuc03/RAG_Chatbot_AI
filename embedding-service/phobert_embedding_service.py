from typing import List

import torch
import torch.nn.functional as F
from transformers import AutoModel, AutoTokenizer


class PhoBertEmbeddingService:
    def __init__(self):
        self.model_name = "vinai/phobert-base"
        self.device = torch.device("cpu")

        print("Đang tải PhoBERT tokenizer...")
        self.tokenizer = AutoTokenizer.from_pretrained(
            self.model_name,
            use_fast=False
        )

        print("Đang tải PhoBERT model...")
        self.model = AutoModel.from_pretrained(self.model_name)
        self.model.to(self.device)
        self.model.eval()

        # PhoBERT/RoBERTa dễ lỗi position embedding nếu input quá dài.
        # Giữ 256 để an toàn cho tài liệu slide/chunk dài.
        self.max_length = 256

    def get_embeddings(self, texts: List[str]) -> List[List[float]]:
        if texts is None or len(texts) == 0:
            return []

        cleaned_texts = []
        for text in texts:
            if text is None or not str(text).strip():
                cleaned_texts.append(" ")
            else:
                cleaned_texts.append(str(text).strip())

        try:
            encoded_input = self.tokenizer(
                cleaned_texts,
                padding=True,
                # RoBERTa/PhoBERT có thể gặp lỗi nếu input quá dài, nên giữ max_length=256 để an toàn
                truncation=True,
                max_length=self.max_length,
                return_tensors="pt"
            )

            encoded_input = {
                key: value.to(self.device)
                for key, value in encoded_input.items()
            }

            with torch.no_grad():
                model_output = self.model(**encoded_input)

            token_embeddings = model_output.last_hidden_state
            attention_mask = encoded_input["attention_mask"]

            input_mask_expanded = attention_mask.unsqueeze(-1).expand(
                token_embeddings.size()
            ).float()

            sum_embeddings = torch.sum(token_embeddings * input_mask_expanded, dim=1)
            sum_mask = torch.clamp(input_mask_expanded.sum(dim=1), min=1e-9)

            sentence_embeddings = sum_embeddings / sum_mask
            sentence_embeddings = F.normalize(sentence_embeddings, p=2, dim=1)

            return sentence_embeddings.cpu().tolist()

        except Exception as e:
            raise RuntimeError(f"PhoBERT embedding failed: {str(e)}")