#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
主程序入口 - 豆包API版本
从input文件夹读取文件，提取表单信息，输出JSON文件到output文件夹
使用豆包API进行信息提取
"""

import os
import sys
import json
import glob
from datetime import datetime
from pathlib import Path
from extractor import InformationExtractor


class MainProcessor:
    """主处理器类"""
    
    def __init__(self, api_key=None, endpoint_id=None, model=None):
        """
        初始化主处理器
        
        Args:
            api_key: 豆包API密钥
            endpoint_id: 接入点ID（可选）
            model: 模型名称（可选）
        """
        # 如果没有传入API key，尝试使用默认的
        if not api_key:
            api_key = "4cb77b4e-734b-4b5f-963a-d9f551213703"
        
        try:
            self.extractor = InformationExtractor(api_key=api_key, endpoint_id=endpoint_id, model=model)
            print("[OK] 豆包API提取器初始化成功")
        except Exception as e:
            print(f"[ERROR] 提取器初始化失败: {e}")
            raise
        
        self._ensure_directories()
    
    def _ensure_directories(self):
        """确保必要的文件夹存在"""
        directories = ["input", "output"]
        for directory in directories:
            if not os.path.exists(directory):
                os.makedirs(directory)
                print(f"创建目录: {directory}")
    
    def process_single_text(self, task, text, output_file=None):
        """
        处理单个文本
        
        Args:
            task: 任务类型，'post' 或 'requirement'
            text: 文本内容
            output_file: 输出文件路径（可选）
            
        Returns:
            提取结果字典
        """
        try:
            if not output_file:
                timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
                output_file = os.path.join("output", f"result_{timestamp}.json")
            
            result = self.extractor.extract(task, text)
            # 只对requirement类型添加元数据，post类型保持简洁
            if task == "requirement":
                result['source_text'] = text[:100] + "..." if len(text) > 100 else text
                result['extraction_time'] = datetime.now().isoformat()
            
            # 保存结果
            with open(output_file, 'w', encoding='utf-8') as f:
                json.dump(result, f, ensure_ascii=False, indent=2)
            
            print(f"  [OK] 结果已保存到: {output_file}")
            return result
            
        except Exception as e:
            print(f"  [ERROR] 处理失败: {e}")
            return None
    
    def process_single_file(self, file_path, task="requirement"):
        """
        处理单个文件
        
        Args:
            file_path: 文件路径
            task: 任务类型，'post' 或 'requirement'
            
        Returns:
            提取结果字典
        """
        try:
            # 读取文件内容
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read().strip()
            
            if not content:
                print(f"  [WARNING] 文件为空: {file_path}")
                return None
            
            print(f"处理文件: {os.path.basename(file_path)}")
            print(f"  文件大小: {len(content)} 字符")
            print(f"  任务类型: {task}")
            
            # 从文件名判断task类型（如果文件名包含post或requirement）
            filename_lower = os.path.basename(file_path).lower()
            if 'post' in filename_lower:
                task = 'post'
            elif 'requirement' in filename_lower:
                task = 'requirement'
            
            # 提取信息
            result = self.extractor.extract(task, content)
            
            # 添加元数据（只对requirement类型添加，post类型保持简洁）
            if task == "requirement":
                result['source_file'] = file_path
                result['file_size'] = len(content)
                result['extraction_time'] = datetime.now().isoformat()
            
            # 保存结果
            base_name = os.path.splitext(os.path.basename(file_path))[0]
            output_file = os.path.join("output", f"{base_name}.json")
            
            with open(output_file, 'w', encoding='utf-8') as f:
                json.dump(result, f, ensure_ascii=False, indent=2)
            
            print(f"  [OK] 处理成功，结果已保存到: {output_file}")
            return result
            
        except FileNotFoundError:
            print(f"  [ERROR] 文件不存在: {file_path}")
            return None
        except Exception as e:
            print(f"  [ERROR] 处理文件时发生错误: {e}")
            return None
    
    def process_input_folder(self, task="requirement"):
        """
        处理input文件夹中的所有文件
        
        Args:
            task: 任务类型，'post' 或 'requirement'
            
        Returns:
            处理结果列表
        """
        print("=" * 60)
        print("豆包信息提取系统")
        print("=" * 60)
        
        # 查找所有文本文件
        input_files = glob.glob(os.path.join("input", "*.txt"))
        
        if not input_files:
            print("在input文件夹中没有找到.txt文件")
            return []
        
        print(f"找到 {len(input_files)} 个文件")
        print(f"任务类型: {task}")
        print()
        
        results = []
        successful = 0
        failed = 0
        
        for i, file_path in enumerate(sorted(input_files), 1):
            print(f"[{i}/{len(input_files)}] ", end="")
            
            result = self.process_single_file(file_path, task)
            
            if result:
                results.append(result)
                successful += 1
            else:
                failed += 1
            print()
        
        # 生成处理报告
        self._generate_report(results, successful, failed)
        
        print(f"\n处理完成！成功: {successful}/{len(input_files)}")
        
        return results
    
    def _generate_report(self, results, successful, failed):
        """生成处理报告"""
        report = {
            "processing_time": datetime.now().isoformat(),
            "total_files": successful + failed,
            "success_count": successful,
            "failure_count": failed,
            "success_rate": f"{(successful/(successful+failed))*100:.1f}%" if (successful+failed) > 0 else "0%",
            "extraction_method": "doubao_api",
            "results": results
        }
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        report_path = os.path.join("output", f"processing_report_{timestamp}.json")
        
        with open(report_path, 'w', encoding='utf-8') as f:
            json.dump(report, f, ensure_ascii=False, indent=2)
        
        print(f"处理报告已保存到: {report_path}")
    
    def show_help(self):
        """显示帮助信息"""
        print("=" * 60)
        print("豆包信息提取系统")
        print("=" * 60)
        print()
        print("使用方法:")
        print("  1. 批量处理input文件夹:")
        print("     python main.py                    # 处理所有文件（默认requirement）")
        print("     python main.py --task requirement # 处理所有文件（requirement类型）")
        print("     python main.py --task post        # 处理所有文件（post类型）")
        print()
        print("  2. 处理单个文本:")
        print("     python main.py <task> <text> [output_file]")
        print("     示例: python main.py requirement '需要帮忙打扫卫生' output.json")
        print()
        print("  3. 显示帮助:")
        print("     python main.py --help 或 python main.py -h")
        print()
        print("功能说明:")
        print("  - 自动读取input文件夹中的所有.txt文件")
        print("  - 使用豆包API提取文本信息（标题、类型、时间、地点等）")
        print("  - 将结果保存到output文件夹")
        print("  - 生成处理报告")
        print()
        print("输出字段:")
        print("  - task: 任务类型（post/requirement）")
        print("  - title: 文本标题总结")
        print("  - type: 服务类型（生活照料类/健康服务类/政务协助类/精神慰藉类/出行辅助类）")
        print("  - serviceTime: 服务时间（HH:MM格式）")
        print("  - estimatedDuration: 服务时长（小时）")
        print("  - warmCoinReward: 暖龄币数量")
        print("  - serviceAddress: 服务地址（my_home/具体地址/null）")
        print()
        print("文件夹结构:")
        print("  input/     - 存放输入文本文件")
        print("  output/    - 存放输出JSON文件")
        print()


def main():
    """主函数"""
    # 解析命令行参数
    show_help = "--help" in sys.argv or "-h" in sys.argv
    
    if show_help:
        processor = MainProcessor()
        processor.show_help()
        return
    
    # 检查是否是单个文本处理模式
    if len(sys.argv) >= 3 and sys.argv[1] in ['post', 'requirement']:
        # 单个文本处理模式
        task = sys.argv[1]
        text = sys.argv[2]
        output_file = sys.argv[3] if len(sys.argv) > 3 else None
        api_key = sys.argv[4] if len(sys.argv) > 4 else None
        endpoint_id = sys.argv[5] if len(sys.argv) > 5 else None
        model = sys.argv[6] if len(sys.argv) > 6 else None
        
        try:
            processor = MainProcessor(api_key=api_key, endpoint_id=endpoint_id, model=model)
            result = processor.process_single_text(task, text, output_file)
            
            if result:
                print("\n提取结果:")
                print(json.dumps(result, ensure_ascii=False, indent=2))
            else:
                print("\n处理失败")
                sys.exit(1)
                
        except Exception as e:
            print(f"错误: {e}")
            sys.exit(1)
    
    else:
        # 批量处理模式
        task = "requirement"  # 默认
        
        # 检查是否有--task参数
        if "--task" in sys.argv:
            task_idx = sys.argv.index("--task")
            if task_idx + 1 < len(sys.argv):
                task = sys.argv[task_idx + 1]
                if task not in ['post', 'requirement']:
                    print(f"错误: task必须是'post'或'requirement'，当前值: {task}")
                    sys.exit(1)
        
        # 创建主处理器
        try:
            processor = MainProcessor()
            
            # 处理文件
            results = processor.process_input_folder(task)
            
            if results:
                print(f"\n处理完成！共处理 {len(results)} 个文件")
                print("请查看output文件夹中的JSON文件")
            else:
                print("\n没有文件被处理")
                print("请确保input文件夹中有.txt文件")
                
        except KeyboardInterrupt:
            print("\n\n用户中断处理")
        except Exception as e:
            print(f"\n程序执行出错: {e}")
            sys.exit(1)


if __name__ == "__main__":
    main()

