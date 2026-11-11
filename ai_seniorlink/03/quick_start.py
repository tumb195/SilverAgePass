"""
快速开始示例
使用提供的API key直接测试
"""
import json
from extractor import InformationExtractor

def main():
    """快速测试"""
    print("=" * 60)
    print("豆包信息提取 - 快速开始")
    print("=" * 60)
    
    # 使用提供的API key
    api_key = "4cb77b4e-734b-4b5f-963a-d9f551213703"
    
    try:
        # 初始化提取器（可以传入endpoint_id，如果使用接入点方式）
        extractor = InformationExtractor(api_key=api_key)
        
        # 测试用例
        test_cases = [
            {
                "task": "requirement",
                "text": "需要帮忙打扫卫生，明天下午2点，1小时，50暖龄币，在我家"
            },
            {
                "task": "requirement", 
                "text": "需要陪护看病，今天上午9点，2小时，在人民医院，60暖龄币"
            }
        ]
        
        for i, test_case in enumerate(test_cases, 1):
            print(f"\n{'='*60}")
            print(f"测试用例 {i}")
            print(f"{'='*60}")
            print(f"Task: {test_case['task']}")
            print(f"Text: {test_case['text']}")
            print("\n正在提取信息...")
            
            try:
                result = extractor.extract(
                    task=test_case['task'],
                    text=test_case['text']
                )
                
                print("\n✓ 提取成功！")
                print("\n提取结果:")
                print(json.dumps(result, ensure_ascii=False, indent=2))
                
                # 保存到文件
                output_file = f"output_{i}.json"
                extractor.extract_to_file(
                    task=test_case['task'],
                    text=test_case['text'],
                    output_file=output_file
                )
                
            except Exception as e:
                print(f"\n✗ 提取失败: {e}")
                print("\n提示：")
                print("1. 检查API key是否正确")
                print("2. 如果使用接入点方式，需要设置endpoint_id")
                print("3. 检查网络连接")
                print("4. 查看extractor.py中的API URL是否正确")
        
    except Exception as e:
        print(f"\n✗ 初始化失败: {e}")
        print("\n请检查：")
        print("1. API key是否正确")
        print("2. 是否安装了所需依赖: pip install -r requirements.txt")

if __name__ == "__main__":
    main()


