# Radar Detector

App Android que mostra os radares de trânsito perto de você num mapa escuro, nas cores verde e preto.

- **Mapa:** [osmdroid](https://github.com/osmdroid/osmdroid) com os blocos *World Dark Gray Base* da Esri e um filtro de cor verde.
- **Radares:** API própria (`GET {RADAR_API_URL}/{lat}/{lon}/{raio em metros}`), que junta o OpenStreetMap (Overpass) com um cadastro local.
- **Localização:** `LocationManager` do próprio Android (não usa Google Play Services).

**Nenhum serviço usado exige chave de API.**

## API de radares

Resposta esperada:

```json
{
  "data": [
    {"type": "node", "id": 8390726476, "lat": -22.8086016, "lon": -46.2650164,
     "tags": {"highway": "speed_camera", "maxspeed": "80", "direction": ""}, "source": "overpass"},
    {"type": "Controlador", "id": 781, "lat": -22.807197, "lon": -46.261658,
     "tags": {"highway": "BR-381", "maxspeed": "80", "direction": "Decrescente"}, "source": "local"}
  ],
  "overpass_count": 1, "local_count": 1, "duplicates_removed": 0
}
```

- O app identifica cada radar por `source:id` (ex.: `local:781`), porque o mesmo `id` pode existir nas duas fontes.
- `tags.highway` diferente de `speed_camera` é mostrado como o nome da via, junto com `tags.direction` (ex.: "BR-381 · Decrescente").
- Itens sem `id`, `lat` ou `lon` válidos são ignorados; uma resposta sem `data` conta como erro.

**Endereço da API** (`BuildConfig.RADAR_API_URL`), nesta ordem:
1. variável de ambiente `RADAR_API_URL` (no CI, a variável do repositório com o mesmo nome);
2. `radarApiUrl=...` no `gradle.properties`;
3. padrão `http://10.0.2.2:8080`, que é o `localhost:8080` do PC visto de dentro do emulador.

**HTTP e rede local:** o Android bloqueia HTTP sem TLS; `res/xml/network_security_config.xml` libera só `10.0.2.2`, `localhost` e `127.0.0.1`. No Android 17+, endereços de rede local (10.x, 192.168.x…) também exigem a permissão **Dispositivos próximos** (`ACCESS_LOCAL_NETWORK`), que o app pede sozinho só quando a API está num desses endereços. Sem ela, as conexões dão timeout.

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
│   ├── enum/                    RadarType e RadarSource
│   ├── model/
│   │   ├── GeoPoint.kt          Coordenada + cálculo de distância (haversine)
│   │   └── Radar.kt             Radar
│   ├── location/
│   │   ├── LocationPermissions.kt  Lista e checagem das permissões
│   │   └── LocationProvider.kt     Posição atual via LocationManager
│   ├── network/
│   │   └── LocalNetworkAccess.kt  Permissão de rede local do Android 17+
│   ├── radar/
│   │   ├── RadarApiClient.kt      GET {url}/{lat}/{lon}/{raio}
│   │   ├── RadarResponseParser.kt Valida e converte o JSON da API em Radar
│   │   └── RadarRepository.kt     Junta os dois
│   └── update/                    Atualizador automático (latest.json + APK)
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
                                └──▶ RadarRepository ──▶ RadarApiClient ──▶ API de radares
```

A UI só lê o `RadarUiState` e chama funções do ViewModel. O ViewModel roda uma busca por vez: uma nova cancela a anterior.

## Como rodar

1. Suba a API de radares no seu PC em `localhost:8080`.
2. Abra o projeto no Android Studio e rode o módulo `app`. Na primeira abertura, permita a localização e "Dispositivos próximos".
3. No emulador, o GPS começa em Mountain View (EUA). Para testar no Brasil, mude a posição:
   - pelo menu **⋯ → Location** do emulador; ou
   - pelo terminal: `adb emu geo fix -46.6333 -23.5505` (longitude primeiro).

   Depois toque em **Atualizar** no app.

## Limitações e cuidados

- **Celular de verdade:** `10.0.2.2` só existe no emulador. Num celular, use o IP do PC na rede (ex.: `radarApiUrl=http://192.168.0.10:8080`, e adicione esse IP ao `network_security_config.xml`) ou `adb reverse tcp:8080 tcp:8080` com `radarApiUrl=http://localhost:8080`.
- **APK publicado:** defina a variável `RADAR_API_URL` no GitHub com o endereço público (HTTPS) da API; sem ela, o APK do CI só funciona no emulador.
- **Erros:** falhas da busca vão para o logcat com a tag `RadarViewModel`.
- **Termos dos blocos do mapa:** os blocos da Esri podem ser usados de forma pessoal ou não comercial, mantendo o crédito na tela (`MapStyle.MAP_ATTRIBUTION`). Antes de publicar comercialmente, revise os termos da Esri.
- **Strings:** todos os textos da tela estão em `res/values/strings.xml`, prontos para tradução.
