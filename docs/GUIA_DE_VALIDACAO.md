# Guia de validação — Voluntariado Hub

O projeto Java 17 organiza turnos e inscrições **fictícias**, com limite de vagas. O núcleo está em `src/VolunteerHub.java` e os testes em `test/VolunteerHubTest.java`.

## Reprodução

```bash
mkdir -p out
javac -Xlint:all -Werror -d out src/VolunteerHub.java test/VolunteerHubTest.java
java -cp out VolunteerHubTest
```

Se a classe de teste usar entrada diferente, seguir o comando exato indicado no README.

## Regressões importantes

- Capacidade exata e excesso de inscrições.
- Reinscrição duplicada, cancelamento e transferência entre turnos.
- Falha de transferência não pode perder a inscrição de origem.
- Concorrência não deve produzir mais inscritos que a capacidade.
- Priorização automática e lista de turnos vazia.

## Próximo incremento

Definir persistência, recuperação de falhas e autorização antes de aceitar informações pessoais de voluntários. Não armazenar nomes, telefones ou documentos reais em testes públicos.
