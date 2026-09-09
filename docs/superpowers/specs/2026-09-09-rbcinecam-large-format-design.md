# RB CineCam Large Format Experimental — Design

## Objetivo
Adicionar ao RB CineCam um modo cinematográfico de grande formato inspirado na linguagem visual de câmeras de grande sensor, sem alegar que o smartphone se torna uma câmera IMAX e sem usar IMAX como nome comercial do recurso.

## Nome
RB Large Format Experimental.

## Formatos
- LF 1.90:1
- LF 1.43:1

## Comportamento
O modo consulta as capacidades reais da câmera física selecionada e tenta montar a melhor configuração disponível. Prioriza 24 fps, shutter angle 180 graus, maior qualidade/resolução estável disponível e maior qualidade de cor disponível. 10-bit, HDR ou LOG só aparecem quando realmente suportados pelo aparelho/câmera/pipeline.

Quando a saída nativa não corresponder ao formato Large Format, o aplicativo usa framing/crop de monitor de forma explícita. O app não deve apresentar crop como captura open-gate.

## Estratégia sensor-aware
O motor deve selecionar a melhor saída real disponível por câmera física. A ordem de preferência é: melhor configuração compatível e estável -> configuração de qualidade inferior compatível -> framing/crop de monitor. Nenhuma opção inexistente deve ser exibida como funcional.

## Estado e fallback
O modo usa o modelo de configuração aplicada da v0.15. Uma configuração solicitada só vira configuração aplicada depois que o bind da câmera tiver sucesso. Em falha, o estado anterior permanece válido e a UI informa o fallback. Nunca fechar o app por combinação de qualidade/FPS não suportada.

## Scopes e compatibilidade
Se ImageAnalysis/scopes impedirem a configuração de maior resolução, o controlador pode tentar novamente sem análise, avisando na interface que os scopes foram desativados para preservar o modo Large Format. Isso deve ocorrer antes de reduzir a resolução.

## Interface
Adicionar seletor `LARGE FORMAT` e opções `LF 1.90` e `LF 1.43`. Durante captura, mostrar de forma compacta o estado realmente aplicado, por exemplo: `LF 1.90 • 4K • 24 • 180° • 10BIT`. A interface continua full-screen, com controles translúcidos e compactos.

## Cor
Prioridade: LOG nativo/10-bit quando realmente disponível, depois HLG/HDR compatível, depois SDR de alta qualidade. RB Log será tratado pelo color engine próprio quando implementado. LUT cinematográfica pode ser usada para monitoramento sem alterar a gravação limpa.

## Metadados
Registrar nos metadados internos do projeto: modo Large Format, proporção de framing, qualidade aplicada, FPS, shutter angle, perfil de cor e câmera/lente. Esses dados devem ser reutilizáveis futuramente pelo RB VideoFire.

## Limites
- Não chamar o recurso de IMAX dentro do produto.
- Não afirmar equivalência técnica com câmeras IMAX.
- Não inventar open-gate, RAW, 10-bit, LOG, 4K/8K ou FPS não oferecidos pelo dispositivo.
- 1.43:1 e 1.90:1 podem ser framing/crop quando não houver saída nativa correspondente.
- A prioridade da v0.15 continua sendo corrigir qualidade/FPS e consolidar o controlador antes de expandir recursos experimentais.

## Critérios de aceite
1. Ativar Large Format não causa crash.
2. A UI sempre reflete a configuração realmente aplicada.
3. LF 1.90 e LF 1.43 produzem guias/framing corretos.
4. 24 fps e 180 graus são priorizados, mas somente se suportados.
5. A maior resolução compatível é tentada sem sacrificar estabilidade.
6. Scopes podem ser desativados automaticamente para preservar uma configuração de captura superior, com aviso ao usuário.
7. O modo volta à configuração anterior se nenhuma tentativa for válida.
8. Testes unitários cobrem seleção, fallback e estado solicitado/aplicado.
