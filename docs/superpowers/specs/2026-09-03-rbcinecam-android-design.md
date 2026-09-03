# RB CineCam Android Design

## Produto

RB CineCam é um aplicativo Android de câmera cinematográfica profissional da RB8 Digital. A primeira linha de desenvolvimento será exclusivamente Android.

## Objetivo da versão 0.1 Alpha

Entregar uma câmera realmente funcional com preview, gravação de vídeo, controles manuais essenciais e leitura das capacidades do dispositivo antes de oferecer qualquer recurso avançado.

## Arquitetura

A aplicação usará Kotlin e Jetpack Compose. CameraX será a camada principal de compatibilidade e lifecycle. Camera2 Interop será usada apenas onde controles profissionais precisarem de acesso mais baixo nível.

Módulos conceituais:

- app: inicialização, navegação, permissões e composição da interface
- camera-core: descoberta de câmeras, capabilities e sessão de preview
- capture: gravação, resolução, fps, codec, bitrate e estado de gravação
- manual-controls: ISO, shutter, foco, white balance, zoom e travas
- monitoring: zebra, histograma, focus peaking e guias
- audio: fonte de áudio, nível, medidor e estado
- media: arquivos capturados, metadata e reprodução
- presets: presets de captura
- color-engine: HDR, 10-bit, LOG, LUT de monitoramento e detecção de perfis
- camera-raw: detecção e futura captura RAW experimental apenas em hardware compatível

## Política de capacidades

O aplicativo nunca apresentará como disponível um recurso não confirmado pelo dispositivo. Resolução, fps, codec, HDR, 10-bit, RAW, LOG, lentes e estabilização serão derivados das APIs do Android e do hardware.

## Captura 0.1 Alpha

A versão inicial deve oferecer:

- preview real
- gravação com áudio
- câmera frontal e traseira
- troca entre lentes físicas quando expostas pelo aparelho
- 1080p e 4K quando suportados
- 24, 30 e 60 fps quando suportados
- H.264 como baseline
- HEVC quando suportado
- ISO manual quando suportado
- shutter manual quando suportado
- foco automático e manual quando suportado
- white balance e trava quando suportados
- zoom
- contador de gravação
- armazenamento restante
- seleção de qualidade
- galeria interna básica

## Monitoramento

A primeira geração profissional deverá suportar zebra, histograma e focus peaking. Essas ferramentas não devem alterar o arquivo gravado.

## LOG e HDR

O color-engine distinguirá:

- SDR padrão
- HLG10/HDR quando exposto pelo dispositivo
- perfis LOG nativos quando o fabricante/API realmente disponibilizar acesso adequado
- RB Log como perfil próprio somente quando tecnicamente consistente e claramente identificado como tal

LUTs poderão ser usadas apenas para monitoramento sem queimar a LUT no arquivo, salvo escolha explícita do usuário em versões futuras.

## RAW Video

RAW Video será experimental e nunca universal. O módulo camera-raw verificará suporte Camera2 RAW, formato, throughput, capacidade de stream, fps e armazenamento antes de liberar qualquer modo. CinemaDNG ou fluxo RAW contínuo só será habilitado quando a combinação puder ser sustentada sem vender suporte fictício.

## Interface

A tela principal será orientada a operação cinematográfica, com preview predominante e controles essenciais nas bordas. Durante gravação, mudanças críticas de codec, resolução e projeto ficarão bloqueadas.

## Erros e segurança operacional

A aplicação deve tratar ausência de permissões, perda de câmera, falha de encoder, armazenamento insuficiente, combinação de streams incompatível e interrupção de gravação. Estados de erro devem ser visíveis e acionáveis.

## Testes

Lógica de capabilities, seleção de qualidade e estados de gravação devem ser testados por unidade. Camera e gravação serão validadas em testes instrumentados e em aparelhos físicos de capacidades diferentes.

## Fora do escopo da 0.1 Alpha

- multicâmera sincronizada
- monitor remoto
- SSD externo avançado
- CinemaDNG contínuo universal
- waveform completo
- false color
- integração final com RB VideoFire

Esses recursos entram depois que o núcleo de captura estiver estável.
