# RB CineCam 0.3.1: estabilização do REC

- Prioridade para VideoCapture: CameraX tenta Preview+ImageCapture+VideoCapture; caso a combinação não seja suportada, tenta Preview+VideoCapture com fallback de qualidade, mantendo a fotografia desabilitada quando necessário.
- O botão REC responde com erro explícito quando não há sessão válida, em vez de parecer sem ação.
- Início, gravação e finalização apresentam mensagens e tratamento do VideoRecordEvent.Finalize.
- Erros de prepareRecording e permissões são capturados e exibidos.
- Estado da câmera e resolução de fallback visíveis; tentativa de recuperação HD.
- Os controles requerem testes físicos no S24 FE. Não declarar gravação homologada apenas por sucesso no CI.
