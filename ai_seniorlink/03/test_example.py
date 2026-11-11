"""
测试示例
"""
import json
from extractor import InformationExtractor


def test_examples():
    """测试示例"""
    extractor = InformationExtractor()
    
    # 测试用例
    test_cases = [
        {
            "task": "requirement",
            "text": "需要帮忙打扫卫生，明天下午2点，1小时，50暖龄币，在我家"
        },
        {
            "task": "requirement",
            "text": "需要陪护看病，今天上午9点，2小时，60暖龄币，在人民医院"
        },
        {
            "task": "post",
            "text": "可以提供做饭服务，时间灵活，每小时25暖龄币"
        },
        {
            "task": "requirement",
            "text": "需要帮忙办理证件，明天10:30，1.5小时，在我家"
        }
    ]
    
    results = []
    for i, test_case in enumerate(test_cases, 1):
        print(f"\n{'='*50}")
        print(f"测试用例 {i}:")
        print(f"Task: {test_case['task']}")
        print(f"Text: {test_case['text']}")
        print(f"{'='*50}")
        
        try:
            result = extractor.extract(test_case['task'], test_case['text'])
            results.append({
                "input": test_case,
                "output": result
            })
            
            print("\n提取结果:")
            print(json.dumps(result, ensure_ascii=False, indent=2))
            
        except Exception as e:
            print(f"错误: {e}")
            results.append({
                "input": test_case,
                "error": str(e)
            })
    
    # 保存所有结果
    with open("test_results.json", "w", encoding="utf-8") as f:
        json.dump(results, f, ensure_ascii=False, indent=2)
    
    print(f"\n所有测试结果已保存到 test_results.json")


if __name__ == "__main__":
    test_examples()


