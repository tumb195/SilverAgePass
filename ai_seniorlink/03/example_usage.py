"""
使用示例
演示如何使用信息提取器
"""
import json
from extractor import InformationExtractor


def example_1():
    """示例1：发需求"""
    print("=" * 60)
    print("示例1：发需求")
    print("=" * 60)
    
    # 可以直接传入API key，或从环境变量读取
    extractor = InformationExtractor()
    
    task = "requirement"
    text = "需要帮忙打扫卫生，明天下午2点，1小时，50暖龄币，在我家"
    
    result = extractor.extract_to_file(task, text, "example1_output.json")
    
    print("\n输入:")
    print(f"  Task: {task}")
    print(f"  Text: {text}")
    print("\n输出:")
    print(json.dumps(result, ensure_ascii=False, indent=2))


def example_2():
    """示例2：发需求 - 超过1小时的服务"""
    print("\n" + "=" * 60)
    print("示例2：发需求 - 超过1小时的服务")
    print("=" * 60)
    
    extractor = InformationExtractor()
    
    task = "requirement"
    text = "需要陪护看病，今天上午9点，2小时，在人民医院，40暖龄币"
    
    result = extractor.extract_to_file(task, text, "example2_output.json")
    
    print("\n输入:")
    print(f"  Task: {task}")
    print(f"  Text: {text}")
    print("\n输出:")
    print(json.dumps(result, ensure_ascii=False, indent=2))
    print("\n注意：如果服务时长超过1小时，暖龄币会自动调整到最低要求（每小时22个）")


def example_3():
    """示例3：发帖子"""
    print("\n" + "=" * 60)
    print("示例3：发帖子")
    print("=" * 60)
    
    extractor = InformationExtractor()
    
    task = "post"
    text = "可以提供做饭服务，时间灵活，每小时25暖龄币，可以上门服务"
    
    result = extractor.extract_to_file(task, text, "example3_output.json")
    
    print("\n输入:")
    print(f"  Task: {task}")
    print(f"  Text: {text}")
    print("\n输出:")
    print(json.dumps(result, ensure_ascii=False, indent=2))


def example_4():
    """示例4：政务协助类需求"""
    print("\n" + "=" * 60)
    print("示例4：政务协助类需求")
    print("=" * 60)
    
    extractor = InformationExtractor()
    
    task = "requirement"
    text = "需要帮忙办理身份证，明天10:30，预计1.5小时，在政务服务中心"
    
    result = extractor.extract_to_file(task, text, "example4_output.json")
    
    print("\n输入:")
    print(f"  Task: {task}")
    print(f"  Text: {text}")
    print("\n输出:")
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    try:
        example_1()
        example_2()
        example_3()
        example_4()
    except Exception as e:
        print(f"\n错误: {e}")
        print("\n请确保已正确配置环境变量：")
        print("  - QIANFAN_ACCESS_KEY")
        print("  - QIANFAN_SECRET_KEY")
        print("\n可以在 .env 文件中配置这些变量")

