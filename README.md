# Hypercube 4D

![hypercube](hypercube-500.gif)

Tesseract — 16 vértices, 32 arestas. Projeção 4D → 3D → 2D com perspectiva. Feito às 4am.

## Arquivos

- `Hypercube.java` — Swing, zero deps, glow por profundidade, 60fps (`javac Hypercube.java && java Hypercube`)
- `hypercube-400.gif` (2.6MB) — otimizado pro GitHub README
- `hypercube-500.gif` (4.1MB) — 500px alta qualidade
- `hypercube.gif` (11MB) — 700px original
- `hypercube.glsl` — shader The Book of Shaders / Shadertoy (GLSL ES 100)
- `hypercube.mp4` — loop pra driftwm/wallpaper

## Rodar (Java)

Da raiz:
```bash
javac hypercube/Hypercube.java -d hypercube && java -cp hypercube Hypercube
```
Dentro da pasta:
```bash
javac Hypercube.java && java Hypercube
```
- drag = scrub no tempo
- click = pause/resume

## Shader

Cole `hypercube.glsl` em https://thebookofshaders.com/edit.php — uniforms `u_time`, `u_resolution`
