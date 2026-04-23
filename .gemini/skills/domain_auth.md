# 權限領域邏輯 (Domain Auth)

## 核心概念
- **User (用戶)**: 系統使用者，具有使用者名稱、加密密碼、電子郵件。
- **Role (角色)**: 綁定用戶的權限集合（如 ADMIN, USER）。角色也用於 Activiti 的 `Candidate Group` 匹配。

## 業務規則
1. **帳號管理**: 
    - 建立用戶時密碼必須經由 `BCrypt` 加密。
    - 支援透過 `assignRolesToUser` 動態調整權限。
2. **通知整合**: 
    - 每個用戶可以綁定 `FcmToken`，用於接受系統的推播通知。
3. **安全過濾**: 
    - 採用無狀態 (Stateless) JWT 認證，Token 存放於 Header 中的 `Authorization`。
