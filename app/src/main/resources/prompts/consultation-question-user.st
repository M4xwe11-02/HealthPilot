# Input Data
请根据以下用户体检报告内容，生成共 {questionCount} 个主问诊问题。
每个主问题必须额外生成 {followUpCount} 个追问。

## 问题分布要求
| 类型 | 数量 | 说明 |
|------|------|------|
| 生活方式 (LIFESTYLE) | {lifestyleCount} 题 | 基于报告异常指标深度了解生活习惯 |
| 营养饮食 (NUTRITION) | {nutritionCount} 题 | 饮食结构、食物偏好、营养摄入等 |
| 运动锻炼 (EXERCISE) | {exerciseCount} 题 | 运动频率、类型、强度等 |
| 症状描述 (SYMPTOM) | {symptomCount} 题 | 主观不适症状、近期身体变化等 |
| 用药情况 (MEDICATION) | {medicationCount} 题 | 当前用药、服药依从性等 |
| 心理健康 (MENTAL_HEALTH) | {mentalHealthCount} 题 | 情绪状态、压力管理等 |
| 睡眠质量 (SLEEP) | {sleepCount} 题 | 入睡难度、睡眠时长、睡眠深度等 |
| 慢性病管理 (CHRONIC_DISEASE) | {chronicDiseaseCount} 题 | 既往病史、家族遗传史、定期复查等 |

## 用户体检报告
---报告内容开始---
{reportText}
---报告内容结束---

## 历史问诊记录
以下是本次问诊中已经询问过的问题列表，请在生成新问题时**尽可能避免重复**（除非是需要从不同维度深入了解同一健康问题）：
---历史问诊开始---
{historicalQuestions}
---历史问诊结束---

## 输出要求
请严格按照 JSON 格式输出问题列表，直接输出 JSON 对象，不要包含任何 Markdown 代码块标签。
