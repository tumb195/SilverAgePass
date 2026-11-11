"""
批量处理input文件夹中的示例文件
"""
import os
import json
import glob
from extractor import InformationExtractor


def process_input_files(input_dir="input", output_dir="output", task="requirement"):
    """
    批量处理input文件夹中的文件
    
    Args:
        input_dir: 输入文件夹路径
        output_dir: 输出文件夹路径
        task: 任务类型，默认为"requirement"
    """
    # 创建输出文件夹
    os.makedirs(output_dir, exist_ok=True)
    
    # 初始化提取器
    api_key = "4cb77b4e-734b-4b5f-963a-d9f551213703"
    extractor = InformationExtractor(api_key=api_key)
    
    # 获取所有txt文件
    input_files = glob.glob(os.path.join(input_dir, "*.txt"))
    
    if not input_files:
        print(f"在 {input_dir} 文件夹中没有找到txt文件")
        return
    
    print(f"找到 {len(input_files)} 个文件，开始处理...\n")
    
    results = []
    
    for input_file in sorted(input_files):
        # 读取文件内容
        with open(input_file, 'r', encoding='utf-8') as f:
            text = f.read().strip()
        
        if not text:
            print(f"跳过空文件: {input_file}")
            continue
        
        # 获取文件名（不含扩展名）
        base_name = os.path.splitext(os.path.basename(input_file))[0]
        output_file = os.path.join(output_dir, f"{base_name}.json")
        
        print(f"{'='*60}")
        print(f"处理文件: {os.path.basename(input_file)}")
        print(f"{'='*60}")
        print(f"文本内容: {text[:100]}..." if len(text) > 100 else f"文本内容: {text}")
        print()
        
        try:
            # 提取信息
            result = extractor.extract(task=task, text=text)
            
            # 保存到JSON文件
            with open(output_file, 'w', encoding='utf-8') as f:
                json.dump(result, f, ensure_ascii=False, indent=2)
            
            print("✓ 提取成功！")
            print(f"✓ 结果已保存到: {output_file}")
            print("\n提取结果:")
            print(json.dumps(result, ensure_ascii=False, indent=2))
            print()
            
            results.append({
                "input_file": input_file,
                "output_file": output_file,
                "text": text,
                "result": result,
                "status": "success"
            })
            
        except Exception as e:
            print(f"✗ 处理失败: {e}")
            print()
            
            results.append({
                "input_file": input_file,
                "output_file": output_file,
                "text": text,
                "error": str(e),
                "status": "error"
            })
    
    # 保存处理摘要
    summary_file = os.path.join(output_dir, "processing_summary.json")
    with open(summary_file, 'w', encoding='utf-8') as f:
        json.dump({
            "total_files": len(input_files),
            "successful": sum(1 for r in results if r["status"] == "success"),
            "failed": sum(1 for r in results if r["status"] == "error"),
            "results": results
        }, f, ensure_ascii=False, indent=2)
    
    print(f"{'='*60}")
    print(f"处理完成！")
    print(f"成功: {sum(1 for r in results if r['status'] == 'success')} 个")
    print(f"失败: {sum(1 for r in results if r['status'] == 'error')} 个")
    print(f"处理摘要已保存到: {summary_file}")
    print(f"{'='*60}")


def main():
    """主函数"""
    import sys
    
    # 可以从命令行参数获取task类型
    task = sys.argv[1] if len(sys.argv) > 1 else "requirement"
    
    if task not in ['post', 'requirement']:
        print("错误: task必须是'post'或'requirement'")
        sys.exit(1)
    
    process_input_files(task=task)


if __name__ == "__main__":
    main()


