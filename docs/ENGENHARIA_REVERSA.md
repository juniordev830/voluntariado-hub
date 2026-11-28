# Engenharia reversa e estudo — Voluntariado Hub

## Objetivo e fluxo

Cadastro de turnos fictícios com controle de vagas. src/VolunteerHub.java reúne os turnos e controla inscrição e capacidade. Os testes permitem acompanhar as regras de entrada antes de expandir a interface.

**Tecnologias identificadas** · Java.

**Revisão de referência** · `e38cdb024e92`.

## Mapa do código

| Caminho | Papel de leitura | Símbolos selecionados |
| --- | --- | --- |
| [src/VolunteerHub.java](../src/VolunteerHub.java) | Implementação | `VolunteerHub`, `ShiftView`, `Shift`, `main`, `token`, `description`, `addShift`, `requireShift`, `enroll` |

## Configuração e referências

Referências existentes: [docs/GUIA_DE_VALIDACAO.md](GUIA_DE_VALIDACAO.md).

## Roteiro de estudo e desenvolvimento

1. Comece pelo fluxo descrito acima e localize seus pontos de entrada no mapa. Acompanhe uma entrada válida até a transformação ou a resposta.
2. Abra um teste relacionado ao fluxo e descreva qual regra ele protege. Compare o tratamento esperado para entradas válidas, inválidas e nos limites.
3. Ao adicionar uma regra, determine qual módulo controla o dado e cubra o caso nesse nível. Em seguida, confira o efeito no comando, na API ou na interface de entrada.

**Pergunta de entrevista**

Como testaria o limite quando a última vaga recebe pedidos quase simultâneos?

## Testes versionados

Arquivos para começar a leitura dos cenários de teste: [test/VolunteerHubTest.java](../test/VolunteerHubTest.java).

Os comandos e versões de runtime usados no CI ficam em [.github/workflows/ci.yml](../.github/workflows/ci.yml).

[Voltar ao README](../README.md).
