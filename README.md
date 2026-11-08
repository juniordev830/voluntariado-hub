# Voluntariado Hub

Primeiro MVP **Java 17** de cadastro de turnos e inscrições de voluntários fictícios, com controle de capacidade. Nenhum dado pessoal real é necessário.

## Entrega

- Criar turno com identificador, nome da ação e capacidade positiva.
- Inscrever voluntários por identificador único dentro do turno.
- Rejeitar inscrição duplicada e terceira pessoa quando restam apenas duas vagas já preenchidas.
- Cancelar uma inscrição, liberando a vaga de forma imediata.
- Consultar vagas livres e inscrições por turno.
- Garantir que chamadas concorrentes no mesmo processo não ultrapassem a capacidade.

## Como executar

Instale JDK 17+, na raiz:

```bash
mkdir -p out
javac -Xlint:all -Werror -d out src/VolunteerHub.java test/VolunteerHubTest.java
java -cp out VolunteerHub
java -cp out VolunteerHubTest
```

A execução demonstrativa cria um turno com duas vagas e dois inscritos fictícios; os testes verificam duplicidade, cancelamento, vagas esgotadas, dados inválidos e concorrência.

## Limites

Por enquanto é uma aplicação em memória e terminal, **não** um sistema público com login. Regras de exclusividade valem somente dentro de uma instância. Persitência PostgreSQL, API Spring Boot, autenticação, múltiplas instâncias e interface acessível são etapas futuras. Não cadastrar nomes, emails ou documentos reais nesse MVP.

## Estado

- [x] Cadastrar ação/turno e capacidade
- [x] Registrar inscrição e cancelamento
- [x] Consultar vagas restantes
- [x] Testar que um turno de 2 vagas impede uma terceira inscrição
- [ ] Persistência e API autenticada
