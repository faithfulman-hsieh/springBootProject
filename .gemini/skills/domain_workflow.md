# 流程領域邏輯 (Domain Workflow)

## 核心概念
- **ProcessDef (流程定義)**: 業務流程的範本，支持動態部署 BPMN。
- **ProcessIns (流程實例)**: 正在運行或已完成的具體申請。
- **Task (任務)**: 流程中需要用戶處理的最小單元。

## 業務規則
1. **任務狀態流轉**: 
    - 待認領 (Unassigned/Candidate) -> 執行 `claim` -> 處理中 (Assigned)。
    - 執行 `unclaim` -> 回到待認領狀態。
2. **表單驅動**: 每個 UserTask 都關聯了 `FormProperty`，定義了前端需渲染的欄位與類型。
3. **跳關限制**: 使用 `JumpCmd` 進行流程跳轉時，禁止在「並行分支 (Parallel Gateway)」中使用，以防死鎖。
4. **歷史追蹤**: 系統記錄所有任務的處理人、耗時與當時的變數狀態。
