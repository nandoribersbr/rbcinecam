# RB CineCam 0.4.2 Alpha – Camera2 manual

- Controles ISO manual, tempo de exposição e presets de balanço de branco via Camera2Interop.
- Capacidades consultadas na lente ativa; ISO/obturador habilitados apenas com MANUAL_SENSOR e ranges informados.
- Presets WB somente se listados por CONTROL_AWB_AVAILABLE_MODES.
- Controles avançados apenas com gravação parada. Retorno de aplicação confirma estado e mostra erro caso hardware não aceite.
- Retorno ao automático remove opções manuais anteriores e ativa AE.
- Bloqueio AE preservado em conjunto com presets AWB quando exposição automática.
- Sem controle Kelvin, LOG, FPS fixo ou garantias de consistência durante sessões de vídeo; requer homologação S24 FE.
