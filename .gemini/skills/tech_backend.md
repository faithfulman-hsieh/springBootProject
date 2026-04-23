# 後端技術基礎 (Tech Backend)

## 核心棧 (Core Stack)
- **Java:** Version 17
- **框架:** Spring Boot 3.3.3
- **工作流:** Activiti 7.1.0.M6
- **安全:** Spring Security 6 + JJWT
- **資料庫:** Spring Data JPA + H2 (Runtime)

## 開發規範
1. **Lombok:** 必須使用 `@Data`, `@Builder`, `@NoArgsConstructor` 等減少樣板代碼。
2. **DTO 模式:** 所有 Controller 請求與響應必須使用 DTO (位於 `.dto` 套件)。
3. **注入方式:** 優先使用構造函數注入 (Constructor Injection)，避免使用 `@Autowired` 於欄位上。
4. **架構分層:** 
    - `adapter/in/rest`: Controller 層。
    - `adapter/out/repository`: Repository 接口。
    - `usecase / service`: 業務邏輯實作。
    - `model`: 領域實體。
