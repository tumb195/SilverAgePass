"""
简单的本地测试脚本：
- 直接调用银发助手模型，看一条回答效果。

使用方式（在项目根目录执行）：
1. 先在环境变量中配置：
   - ARK_ACCESS_TOKEN   豆包/Ark 的 Access Token
   - ARK_MODEL_ID       在控制台上的模型 ID 或应用 ID
   - 可选：ARK_CHAT_COMPLETIONS_URL（如与默认值不同）
2. 运行：
   - Windows PowerShell:  python -m backend.test_silver_helper
"""

from backend.ark_client import get_default_client


def main() -> None:
    client = get_default_client()

    # 你可以在这里改成你想测试的问题
    question = "我今年72岁，最近晚上总是睡不踏实，半夜会醒几次，第二天很困，该怎么调整生活习惯？"

    print("用户：", question)
    answer = client.chat(user_text=question, history=None)
    print("\n银发助手：")
    print(answer)


if __name__ == "__main__":
    main()


