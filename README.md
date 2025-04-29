# 📘 KcLoginGui — Registro e Login GUI para Bedrock (via Floodgate)

## 🔄 Atualizações e Melhorias Recentes

### ✅ 1. UX aprimorada no Login e Registro
- Sem kick agressivo ao errar a senha ou ao digitar errado na confirmação:
  - Jogador recebe uma mensagem clara no chat.
  - A janela de login ou registro se reabre automaticamente após erro.
- Alinhado com o comportamento definido no `config.yml`:
  ```yaml
  kickOnWrongPassword: false
  ```

### ✅ 2. Validações extras no registro
- Verificação do:
  - Comprimento mínimo e máximo da senha, respeitando o `AuthMe/config.yml`:
    ```yaml
    minPasswordLength: 4
    passwordMaxLength: 26
    ```
  - Caracteres válidos com base na expressão:
    ```yaml
    allowedPasswordCharacters: '[!-~]*'
    ```
- Mensagens de erro totalmente integradas ao `config.yml`:
  ```yaml
  password-too-short: "&cPassword must be between %min% and %max% characters."
  password-invalid-chars: "&cPassword contains invalid characters. Only ASCII printable characters are allowed."
  ```

### ✅ 3. Comportamento adaptado para usuários mobile (Floodgate/Bedrock)
- Consideradas as variações de comandos digitados com letras maiúsculas no celular:
  ```yaml
  allowCommands:
    - /login
    - /Login
    - /register
    - /Register
    - /l
    - /reg
  ```
- Garante compatibilidade com o comportamento do teclado automático de celulares.

### ✅ 4. Empacotamento correto das dependências com Maven
- `foliascheduler` incluído no `.jar` final via `maven-shade-plugin`:
  ```xml
  <plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    ...
  </plugin>
  ```
- Evita erros como `NoClassDefFoundError: com/cjcrafter/foliascheduler/util/ServerVersions`.

### ✅ 5. Comportamento configurado do AuthMe
- Redução de mensagens repetitivas no chat:
  ```yaml
  registration:
    messageInterval: 10
  ```
- Evita flood de mensagens ao mover o jogador:
  ```yaml
  settings:
    reauthenticate:
      enableMovementCheck: false
  ```

## 📌 Resultado Final
KcLoginGui agora proporciona:
- Experiência mais amigável para jogadores Bedrock
- Validações seguras e personalizáveis
- Compatibilidade com teclado de celular
- Menor poluição visual no chat
- Maior estabilidade com dependências corretamente empacotadas