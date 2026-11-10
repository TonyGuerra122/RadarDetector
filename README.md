# Radar Detector

App Android que mostra os radares de trânsito perto de você num mapa escuro, nas cores verde e preto.

- **Mapa:** [osmdroid](https://github.com/osmdroid/osmdroid) com os blocos *World Dark Gray Base* da Esri e um filtro de cor verde.
- **Radares:** nós `highway=speed_camera` do OpenStreetMap, consultados pela [API Overpass](https://wiki.openstreetmap.org/wiki/Overpass_API).
- **Localização:** `LocationManager` do próprio Android (não usa Google Play Services).

**Nenhum serviço usado exige chave de API.**

## Funcionalidades

- Busca de radares num raio de 1, 3, 5 ou 10 km em volta da sua posição.
- Lista ordenada pela distância, com o limite de velocidade (quando cadastrado) e o tipo de radar.
- Tocar num radar da lista ou do mapa destaca o radar e aproxima o mapa.
- Sem permissão de localização, a busca usa o centro de São Paulo.

## Estrutura

```
com.tonyguerra.radardetector
├── MainActivity.kt              Única Activity; mostra a RadarScreen
├── data/                        Sem dependência de UI nem da biblioteca de mapa
│   ├── model/
│   │   ├── GeoPoint.kt          Coordenada + cálculo de distância (haversine)
│   │   └── Radar.kt             Radar e RadarType
│   ├── location/
│   │   ├── LocationPermissions.kt  Lista e checagem das permissões
│   │   └── LocationProvider.kt     Posição atual via LocationManager
│   └── radar/
│       ├── OverpassClient.kt    HTTP para a Overpass, com servidores reserva
│       └── RadarRepository.kt   Monta a consulta e converte o JSON em Radar
└── ui/
    ├── map/                     Tudo que depende do osmdroid
    │   ├── MapStyle.kt          Fonte dos blocos, filtro verde, zoom, crédito
    │   ├── RadarMap.kt          Composable que envolve o MapView
    │   ├── MarkerIcons.kt       Ícones dos marcadores desenhados em código
    │   └── CameraRequest.kt     Pedido de movimento da câmera
    ├── radar/                   Tela principal
    │   ├── RadarScreen.kt       RadarScreen (com estado) + RadarContent (sem estado) + Preview
    │   ├── RadarViewModel.kt    Localização, busca e seleção
    │   ├── RadarUiState.kt      Estado imutável da tela
    │   ├── RadarFormatters.kt   Distância e nome do tipo de radar
    │   └── components/          Cabeçalho, painel da lista, item da lista, indicadores
    └── theme/                   Paleta verde e preto (Material 3, sempre escuro)
```

### Fluxo de dados

```
RadarScreen ──permissão──▶ RadarViewModel ──▶ LocationProvider
     ▲                          │
     │   StateFlow<RadarUiState>│
     └──────────────────────────┤
                                └──▶ RadarRepository ──▶ OverpassClient ──▶ Overpass API
```

A UI só lê o `RadarUiState` e chama funções do ViewModel. O ViewModel roda uma busca por vez: uma nova cancela a anterior.

## Como rodar

1. Abra o projeto no Android Studio e rode o módulo `app`.
2. No emulador, o GPS começa em Mountain View (EUA). Para testar no Brasil, mude a posição:
   - pelo menu **⋯ → Location** do emulador; ou
   - pelo terminal: `adb emu geo fix -46.6333 -23.5505` (longitude primeiro).

   Depois toque em **Atualizar** no app.

## Limitações e cuidados

- **Cobertura dos dados:** os radares vêm de cadastros voluntários no OpenStreetMap. Uma região sem resultados pode só não ter cadastro.
- **Servidores Overpass públicos:** são gratuitos, mas às vezes ficam lentos ou devolvem 504. O app tenta três servidores em sequência e registra as falhas no logcat (tags `OverpassClient` e `RadarViewModel`).
- **Termos dos blocos do mapa:** os blocos da Esri podem ser usados de forma pessoal ou não comercial, mantendo o crédito na tela (`MapStyle.MAP_ATTRIBUTION`). Antes de publicar comercialmente, revise os termos da Esri.
- **Strings:** todos os textos da tela estão em `res/values/strings.xml`, prontos para tradução.
