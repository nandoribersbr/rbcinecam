# RB CineCam 0.4.1 Alpha

- Foco por toque com indicador na pré-visualização e confirmação do resultado.
- Bloqueio AF mediante FocusMeteringAction.disableAutoCancel; cancelFocusAndMetering para retornar a foco automático.
- Bloqueio AE via Camera2Interop CaptureRequest.CONTROL_AE_LOCK apenas se CONTROL_AE_LOCK_AVAILABLE.
- Exibição de compensação em unidades EV utilizando exposureCompensationStep.
- Reset de bloqueios na reinicialização da sessão CameraX.
- Barra Ferramentas inclui AF e AE, com indicadores de estado; os recursos ficam inativos quando indisponíveis.
- Gravação CameraX, pausa/retomada e galeria preservadas.
- Aplicação dos bloqueios e comportamento contínuo ainda requerem testes reais no Galaxy S24 FE.
