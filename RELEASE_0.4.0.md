# RB CineCam 0.4.0 Alpha

Base preservada: CameraX, gravação FHD validada em vídeo real, galeria e interface de toque da 0.3.3.

Alterações:
- Pausar/retomar com Recording.pause()/resume() e eventos de confirmação Pause/Resume
- Permitir parar mesmo se vídeo estiver pausado
- Cronômetro congelado durante pausa
- HUD compacto com espaço livre aproximado e bytes gravados
- Diagnóstico de toques fica opcional no painel de configurações
- Impedir ajustes de lente, captura fotográfica e áudio durante gravação/pausa
- Testes unitários para estados de pausa/retomada

Pendências: validar pausa e retomada no Galaxy S24 FE, 10 vídeos consecutivos, gravação prolongada, codecs e 4K. O build CI não comprova esses cenários.
