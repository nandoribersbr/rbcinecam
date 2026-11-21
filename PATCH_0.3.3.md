# Patch 0.3.3 diagnóstico de toques
- A Activity conta cada evento ACTION_DOWN recebido e mostra contagem no rodapé.
- Cada botão mostra sua identificação no rodapé ao ser acionado.
- REC tem log específico e feedback antes da chamada CameraX.
- Controle lateral, barra inferior e barra superior ganham zIndex acima do PreviewView.
- A hipótese de interceptação de toques ainda requer confirmação em dispositivo real.
- Se o contador de toques não aumentar, o Android não está entregando os eventos à Activity; se aumentar e o nome do botão não mudar, há bloqueio na distribuição de toques entre View/Compose.
- Compilação CI não atesta gravação em dispositivo.
