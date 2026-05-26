# Debug Session: supply-chain-tag-500 [OPEN]

## Context
- Symptom: `/api/biz/supply-chain-tag/list?page=1&size=12` and `/api/biz/supply-chain-tag/tree` return `{"code":500,"msg":"系统内部错误，请联系管理员"}`
- Expected: both endpoints return normal data for the supply-chain tag page
- Scope: backend business module + frontend supply-chain tag page data loading

## Initial Hypotheses
1. `BizSupplyChainTag` entity field mapping does not match table schema, causing MyBatis result mapping failure.
2. Logic-delete or auto-fill fields are misconfigured, causing query execution to fail at runtime.
3. Controller tree/list response building touches a null or incompatible property and throws during serialization/buildTree.
4. Query conditions against `tagPath` / pagination execute, but the returned entity contains unsupported field types or missing getters.
5. The table has data, but one or more rows contain values incompatible with entity field types.

## Evidence Plan
- Reproduce endpoint failures with authenticated requests
- Capture backend stack trace / exception class
- Inspect entity, mapper, service, and controller path involved in both endpoints
- Add minimal instrumentation only if existing logs are insufficient

## Progress Log
- Session created
