# Integração inicial RB CineCam

Esta branch cria o aplicativo Android; antes dela, o repositório continha apenas documentação.

## Controles implementados em código
- Preview CameraX com solicitação de permissão
- Gravação MP4 com CameraX VideoCapture / MediaStore, iniciar/parar, áudio opcional
- Captura fotográfica JPEG
- Troca de câmera traseira/frontal quando detectada
- Zoom com limite reportado pelo hardware
- Foco por toque via FocusMeteringAction
- Grade dos terços ativável
- Abertura do visualizador externo de vídeo (não é galeria interna)
- Seletor 1080p/4K condicionado a supportedQualities
- Configurações e informações com fechamento funcional

## Não implementados / não expostos como botões ativos
ISO manual, shutter, temperatura de cor manual, histograma, waveform, zebra, peaking, LOG, HDR, foco manual, seleção arbitrária de FPS e codec.

## Validação
O workflow executará compilação e testes unitários após push. A validação física no Samsung Galaxy S24 FE permanece pendente. Não apresentar como concluídos testes que ainda não executaram.
