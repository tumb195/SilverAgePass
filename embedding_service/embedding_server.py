"""
BGE-M3 Embedding Service
使用 ModelScope 镜像下载模型，避免 HuggingFace 网络问题
"""
import os
import json
from flask import Flask, request, jsonify

# 设置 ModelScope 镜像（国内网络友好）
os.environ['HF_ENDPOINT'] = 'https://hf-mirror.com'

from sentence_transformers import SentenceTransformer

app = Flask(__name__)

# 使用较小的中文 embedding 模型，下载更快
# 如果网络好可以换成 'BAAI/bge-m3'
MODEL_NAME = os.environ.get('EMBEDDING_MODEL', 'BAAI/bge-small-zh-v1.5')
print(f"Loading model: {MODEL_NAME}")
model = SentenceTransformer(MODEL_NAME)
print(f"Model loaded successfully! Dimension: {model.get_sentence_embedding_dimension()}")


@app.route('/embed', methods=['POST'])
def embed():
    """单文本嵌入"""
    data = request.json
    text = data.get('inputs', '')
    if isinstance(text, list):
        text = text[0] if text else ''
    
    embedding = model.encode(text).tolist()
    return jsonify([[embedding]])


@app.route('/embed_batch', methods=['POST'])
def embed_batch():
    """批量文本嵌入 - 兼容 Spring Boot 调用格式"""
    data = request.json
    texts = data.get('inputs', [])
    if isinstance(texts, str):
        texts = [texts]
    
    embeddings = model.encode(texts).tolist()
    return jsonify(embeddings)


@app.route('/embeddings', methods=['POST'])
def embeddings():
    """OpenAI兼容格式的嵌入接口 - 供Spring Boot RAG服务调用"""
    data = request.json
    texts = data.get('input', [])
    if isinstance(texts, str):
        texts = [texts]
    
    embeddings_list = model.encode(texts).tolist()
    
    # 返回OpenAI兼容格式
    response = {
        "object": "list",
        "data": [
            {
                "object": "embedding",
                "index": i,
                "embedding": emb
            }
            for i, emb in enumerate(embeddings_list)
        ],
        "model": MODEL_NAME,
        "usage": {
            "prompt_tokens": sum(len(t) for t in texts),
            "total_tokens": sum(len(t) for t in texts)
        }
    }
    return jsonify(response)


@app.route('/health', methods=['GET'])
def health():
    """健康检查"""
    return jsonify({
        'status': 'healthy',
        'model': MODEL_NAME,
        'dimension': model.get_sentence_embedding_dimension()
    })


@app.route('/', methods=['GET'])
def info():
    """服务信息"""
    return jsonify({
        'service': 'BGE Embedding Service',
        'model': MODEL_NAME,
        'dimension': model.get_sentence_embedding_dimension(),
        'endpoints': ['/embed', '/embed_batch', '/embeddings', '/health']
    })


if __name__ == '__main__':
    port = int(os.environ.get('PORT', 8001))
    print(f"Starting embedding service on port {port}...")
    app.run(host='0.0.0.0', port=port, threaded=True)
