# NEXORA

Seu celular no melhor desempenho.

App Android nativo, em português, para acompanhar bateria, armazenamento interno e memória RAM. Primeira versão: **0.2.0**, Android **8.0 ou superior** (API 26).

## Baixar para Android

[Baixar NEXORA 0.2.0 — APK de teste](https://github.com/JJunior88/NEXORA/raw/refs/heads/main/downloads/nexora-0.2.0-debug.apk)

Requer Android 8.0 ou superior. Este APK tem assinatura de desenvolvimento e ainda precisa de validação em aparelho físico. Para instalar, permita a instalação pelo navegador ou gerenciador de arquivos quando o Android solicitar. O checksum SHA-256 está em `downloads/SHA256SUMS`.

## Cuidados e ações na versão 0.2.0

A aba **Cuidar** analisa as leituras e mostra recomendações para temperatura elevada, espaço disponível inferior a 10%, pressão de memória, bateria baixa e alertas de saúde da bateria. Leituras ausentes não são consideradas saudáveis. RAM ocupada sozinha não gera alerta.

- **Perfil econômico de tela:** com autorização opcional de `WRITE_SETTINGS` concedida pelo usuário na tela do Android, aplica brilho manual de até 30% e suspensão de tela de até 30 segundos. Valores já menores são mantidos. Os valores anteriores são salvos antes da alteração; **Restaurar ajustes anteriores** recupera os valores, preservando alterações posteriores quando detectáveis. Isso pode reduzir consumo de tela, mas não aumenta CPU/RAM e nenhum ganho de autonomia foi medido.
- **Persistência dos ajustes:** o perfil muda configurações do sistema que continuam após fechar ou desinstalar o app. Restaure antes de desinstalar ou ajuste a tela no próprio Android. Revogar a permissão impede restauração até concedê-la novamente; o app mantém os valores anteriores salvos. Não confunda isso com um serviço de otimização contínua.
- **Limpeza local:** remove apenas arquivos descartáveis de `getCacheDir()` do NEXORA em uma thread de trabalho. Não segue links simbólicos, não limpa caches de outros apps, nem fotos, downloads ou dados de usuário. Mostra a quantidade de arquivos, tamanho removido e falhas. Pode remover zero bytes; isso não significa aceleração.
- **Manutenção automática opcional:** desativada por padrão, limpa o cache do NEXORA ao abrir o app, no máximo uma vez a cada seis horas. Não altera o perfil de tela automaticamente e não mantém serviço de monitoramento em segundo plano. O diagnóstico é atualizado enquanto o painel está aberto.
- **Apps com falha:** orientações e atalhos para gerenciamento de apps e atualizações do Android. O NEXORA não detecta ou corrige erros internos de outros apps, não repara hardware, não força parada de processos e não promete “liberar RAM”.

## O que funciona

- **Visão geral:** bateria, espaço disponível e RAM em uso.
- **Bateria:** carga, estado de carregamento, temperatura, tensão, saúde reportada e economia de energia.
- **Armazenamento:** capacidade, espaço ocupado e disponível do volume interno de dados; atalho para o gerenciador do Android.
- **Desempenho:** RAM total/disponível, pressão de memória, modelo, versão do Android e estado térmico (Android 10+).
- Atualização ao retornar ao app, a cada 30 segundos em primeiro plano e pelo botão **Atualizar agora**.
- Abas preservadas na recriação da atividade; interface adapta-se às barras do sistema e permite rolagem.

O app usa APIs públicas do Android, sem conta, anúncios, rastreadores ou acesso à internet. O monitoramento não exige permissões especiais; somente o perfil de tela pede autorização opcional para alterar configurações. A limpeza exclui apenas o cache do próprio NEXORA. Alterações de outros apps e exclusão de arquivos pessoais ficam nas configurações do Android, sob controle do usuário.

Leituras ausentes aparecem como indisponíveis. A saúde da bateria não representa a porcentagem de capacidade original. RAM ocupada não determina lentidão; o Android gerencia memória automaticamente. O armazenamento exibido corresponde ao volume de dados, não a cartões SD, nuvem ou à capacidade comercial anunciada. Não há medição de uso de CPU ou estimativa de autonomia.

## Abrir e compilar

No Android Studio, abra esta pasta e sincronize o Gradle. Instale o SDK Android 35 e Build Tools 35.0.0 pelo SDK Manager. Use JDK 21 (código Java 17) e o Gradle Wrapper incluído.

Pelo terminal, configure `ANDROID_HOME` para seu SDK e `JAVA_HOME` para o JDK. Caso prefira, informe `sdk.dir` em um `local.properties` local (ignorado pelo Git).

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

APK de desenvolvimento: `app/build/outputs/apk/debug/app-debug.apk`.

Com um aparelho conectado por USB, depuração habilitada e `adb` no PATH:

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.nexora.app/.MainActivity
```

O APK debug serve para testes. Distribuição pública exige uma chave própria de assinatura e configuração de release; não há credenciais de assinatura neste repositório.

## Testes

`MetricsTest` verifica escala de bateria, leituras desconhecidas, unidades em português e valores grandes de armazenamento. `MainActivityTest` usa Robolectric (Android 8, 9 e 15 simulados) para verificar navegação entre as quatro áreas, leitura da bateria, atalho de economia de energia e recriação da atividade. Os testes de diagnóstico cobrem alertas e dados ausentes; os de limpeza verificam cache aninhado, idempotência e isolamento de arquivos pessoais e links simbólicos; os de perfil verificam permissão negada/revogada, aplicação, restauração, valores menores e preservação de alterações do usuário; os de manutenção verificam opt-in e intervalo. O CI executa testes, Android Lint e build debug e disponibiliza o APK como artefato.

Antes de distribuir, valide em aparelho físico: as cinco telas, concessão e revogação da permissão de configurações, aplicação e restauração do perfil, limpeza manual e automática, atualização após conectar/desconectar o carregador, atalhos das configurações, TalkBack, fonte ampliada e rotação. Fabricantes podem disponibilizar leituras diferentes. Testes em JVM não substituem essa validação.

## Ambiente na nuvem

Os comandos abaixo usam ferramentas locais em `/workspace/toolchains`, sem alterar o Java global:

```bash
bash scripts/setup-cloud.sh
source scripts/cloud-env.sh
./gradlew --no-daemon --max-workers=2 testDebugUnitTest lintDebug assembleDebug
```

O setup verifica checksums oficiais antes de extrair JDK e ferramentas Android, instala o SDK e configura o proxy somente no diretório local do Gradle. O ambiente não precisa de servidor. Para usar a interface, instale o APK em um Android ou abra o projeto no Android Studio.
