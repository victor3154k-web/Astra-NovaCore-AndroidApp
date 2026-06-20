# 🌌 Astra NovaCore Player

> Um player de vídeo Android de ultra-alta performance, focado em decodificação pesada, customização de estilo LED Neon e sincronia avançada de mídias.

---

## 🚀 Sobre o Projeto

O **Astra NovaCore Player** foi desenvolvido utilizando as técnicas mais modernas de engenharia no ecossistema Android. Unindo a beleza do **Jetpack Compose (Material Design 3)** à robustez de uma **Arquitetura VLC & FFmpeg**, o Astra é capaz de reproduzir desde simples formatos de celular até complexos fluxos UHD (4K) em HEVC/H.265 sem nenhuma trava.

---

## ✨ Principais Diferenciais

```
🌌 Astra NovaCore Architecture:
┌────────────────────────────────────────────────────────┐
│                    Jetpack Compose UI                  │
├───────────────────────────┬────────────────────────────┤
│   ⚡ MobileVLCKit Engine   │   ⚙️ FFmpeg Core Backend    │
│   (Codecs & Containers)   │ (Software Decoding Threads)│
└───────────────────────────┴────────────────────────────┘
```

### 1. 🛠️ Motores MobileVLCKit & FFmpeg (Dual Mode)
O reprodutor utiliza as robustas bibliotecas **MobileVLCKit** e o motor decodificador **FFmpeg** integrado:
- **MobileVLCKit**: Fornece a infraestrutura de alto nível para renderização acelerada por hardware (HW), controle de fluxo, legendas estendidas e áudio de alta fidelidade.
  - *Identidade visual descrita:* `"MobileVLCKit : Uma Bliblioteca de Alta Performance."`
- **FFmpeg Engine Backend**: Atua como o núcleo de decodificação de software (SW) multi-threaded, trazendo suporte completo a praticamente todos os codecs e contêineres de vídeo e áudio existentes no planeta.
  - *Identidade visual descrita:* `"FFmpeg : Essa Bliblioteca Focada em compatibilidade melhor."`

### 2. 🔊 Amplificador de Áudio Booster (Até 300%)
Aumente o volume de áudio drasticamente além do limite padrão do smartphone:
- Amplificação em escala contínua até **300%** integrada em tempo real.
- Caixa flutuante acionada via botão de três pontos (`MoreVert`) no controle do player.
- Botões de atalho rápido e gradativos (**100%**, **150%**, **200%**, e **300%**) com suavização acústica por software.

### 3. 🎨 Visual Customizável Independente (Aba Temas)
Diferente das configurações tradicionais, a aba **Temas** funciona de maneira 100% autônoma e centralizada na barra de navegação inferior do Dashboard de controle principal:
- **Preview Interativo Geral**: Um modelo realista com aura pulsante animada exibe na hora os efeitos de LED Neon ativos.
- **Galeria de Atmosferas**:
  - `Luxo Aura Gold`: Elegância executiva cromada nobre com brilho metálico.
  - `LED Sunset Neon`: Nuance quente, energética e aconchegante de sol poente.
  - `LED Cyber Frio`: Atmosfera gélida cibernética minimalista futurista (azul/verde ciano).
  - `Multi-Luzes RGB`: Transição cromática dinâmica de espectro arco-íris ondulatório.
- **Ajuste de Difusão**: Controle o tamanho e dispersão do brilho neon LED (5dp a 30dp) e ative a respiração de pulso rítmico contínuo.

### 4. ☁️ Portal Cloud Avançado (Auth Cloud Media Sync)
Uma central avançada de captação e indexação de vídeos remotos com melhorias profundas de estabilidade:
- **Indexador Inteligente**: Rastreie recursivamente diretórios inteiros apontados por caminhos do GitHub ou processe diretamente playlists do tipo `.m3u` / `.m3u8` carregando canais públicos.
- **Personal Access Token**: Insira seu token do GitHub `ghp_...` para evitar limites de taxa de chamadas de IP impostas pela API pública, garantindo sincronização sem bloqueios para repositórios gigantescos.
- **Terminal de Logging Ativo**: Caixa estilo prompt de comando que deita logs reais em tempo real enquanto o motor rastreia as pastas e arquivos de mídias conectadas.
- **Bookmark de Conexões (Histórico)**: Salve conexões bem-sucedidas automaticamente. Com um único toque, você pode restaurar e varrer novamente o repositório salvo para refrescar sua biblioteca sem digitar tudo de novo.

### 5. 🧠 Alocação Dinâmica de CPUs & Limpeza de Memória
Controle cirúrgico do hardware do seu dispositivo:
- Aloque o número exato de cores físicos do processador de forma nativa para prevenir aquecimentos térmicos.
- Otimize a memória RAM instantaneamente clicando em limpar cache.

---


## 🛠️ Tecnologias Utilizadas

- **Kotlin** para código dinâmico e seguro.
- **Jetpack Compose** com Material Design 3.
- **Room Database** para persistência física rápida.
- **Coroutines & Flow** para gerenciar threads com segurança.
- **MobileVLCKit & FFmpeg Core** para reprodução multimídia profissional.

---
*Astra NovaCore Player: Redefinindo o significado de reprodução e estilo no Android.* 🚀🌌
