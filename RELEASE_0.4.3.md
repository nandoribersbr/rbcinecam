# RB CineCam 0.4.3 Alpha

- VU meter real durante gravação: AudioStats.audioAmplitude de VideoRecordEvent.Status; escala dBFS aproximada, 48 barras segmentadas com verde/amarelo/vermelho.
- Indica SEM PERMISSÃO, MIC OFF, PAUSA, MEDIÇÃO AO GRAVAR e SEM SINAL, sem animar amplitude fictícia.
- Modo HLG 10-bit HDR opcional condicionado a getSupportedDynamicRanges, com retorno automático a SDR quando CameraX não aceita a combinação.
- Consulta suporte RAW fotográfico da câmera (somente capacidade, não DNG ou vídeo RAW).
- Consulta tamanhos de saída >=8K (somente capacidade; não exporta ou amplia vídeos).
- Nenhum recurso RAW de vídeo, LOG ou upscale 8K é anunciado como ativo: ainda requer implementação real.
- Antes de release: homologar vídeo HDR, cores, amplitude e salvamento no Galaxy S24 FE.
