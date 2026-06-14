# Sử dụng bản sao Python rút gọn để tối ưu dung lượng
FROM python:3.10-slim

# Thiết lập thư mục làm việc bên trong container
WORKDIR /app

# Sao chép file danh sách thư viện vào trước để tận dụng cache Docker
COPY requirements.txt .

# Cài đặt các thư viện Python cần thiết
RUN pip install --no-cache-dir -r requirements.txt

# Sao chép toàn bộ mã nguồn của embedding-service vào container
COPY . .

# Mở cổng 8000 để các dịch vụ khác có thể gọi tới API này
EXPOSE 8000

# Lệnh khởi chạy API thông qua uvicorn khi container bắt đầu chạy
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]