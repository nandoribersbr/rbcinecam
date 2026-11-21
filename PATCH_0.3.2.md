# Patch 0.3.2: REC e HUD

- Estados separados: IDLE, PREPARING, RECORDING, STOPPING, ERROR.
- Toque em REC gera resposta imediata; callbacks Start, Status e Finalize atualizam a interface.
- Cronômetro mm:ss e indicador REC no topo; botão vermelho destacado e rótulos por estado.
- Falhas apresentadas no rodapé e registradas no Logcat (tag RBCineCam), com código CameraX e causa quando disponível.
- Timeout de 12 segundos para preparação de gravação.
- Testes unitários para transições; CI assembleDebug e testDebugUnitTest.
- Este patch não certifica captura real em S24 FE: requer gravação de 10 segundos e inspeção do arquivo MP4.
