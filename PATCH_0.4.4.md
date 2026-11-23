# RB CineCam 0.4.4

Corrige monitor invisível ao posicioná-lo sobre o visor em primeiro plano, com zIndex 20. Mede áudio real em pré-visualização usando AudioRecord e libera o microfone quando inicia REC; durante gravação, usa AudioStats do CameraX. Sem valores simulados. Confirmar transição de fontes e ausência de competição pelo microfone no Galaxy S24 FE.
