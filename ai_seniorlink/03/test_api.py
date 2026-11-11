"""
测试豆包API配置
"""
import os
from extractor import InformationExtractor

def test_api_config():
    """测试API配置"""
    print("=" * 60)
    print("测试豆包API配置")
    print("=" * 60)
    
    # 方式1：从环境变量读取（推荐）
    try:
        extractor = InformationExtractor()
        print("✓ 成功从环境变量读取配置")
    except ValueError as e:
        print(f"✗ 环境变量配置失败: {e}")
        print("\n尝试使用直接传入API key...")
        
        # 方式2：直接传入API key
        api_key = "4cb77b4e-734b-4b5f-963a-d9f551213703"
        try:
            extractor = InformationExtractor(api_key=api_key)
            print("✓ 成功使用直接传入的API key")
        except Exception as e2:
            print(f"✗ 直接传入API key也失败: {e2}")
            return
    
    # 测试一个简单的提取
    print("\n测试信息提取...")
    try:
        result = extractor.extract(
            task="requirement",
            text="需要帮忙打扫卫生，明天下午2点，1小时，50暖龄币"
        )
        print("✓ 信息提取成功！")
        print("\n提取结果:")
        import json
        print(json.dumps(result, ensure_ascii=False, indent=2))
    except Exception as e:
        print(f"✗ 信息提取失败: {e}")
        print("\n提示：")
        print("1. 检查API key是否正确")
        print("2. 检查网络连接")
        print("3. 如果使用接入点方式，需要设置DOUBAO_ENDPOINT_ID")
        print("4. 如果API URL不正确，可能需要调整extractor.py中的api_url")

if __name__ == "__main__":
    test_api_config()


