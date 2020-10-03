# Estufa-Trabalho-Redes
Nesta aplicação as condições da estufa são conﬁguradas, monitoradas e controladas através do gerenciador. O gerenciador se comunica com os sensores/atuadores através de um protocólo de comunicação na rede. Além disso, o gerenciador pode receber conﬁgurações(Como definir temperatura estufa) ou responder consultas de um cliente externo das condições da estufa. No arquivo [Proposta_Trabalho_Redes.pdf](https://github.com/EwertonPSA/Estufa-Trabalho-Redes/blob/master/Proposta_Trabalho_Redes.pdf) é apresentado mais detalhes sobre o escopo do projeto e os requisitos a serem desenvolvidos para a aplicação.

Este trabalho foi desenvolvido em duas etapas: 
1) Definir um protocolo de comunicação entre os sensores, atuadores e gerenciador.
2) Desenvolver a aplicação em uma linguagem de programação. 

No arquivo [protocolo_Comunicação_Equipamento.pdf](https://github.com/EwertonPSA/Estufa-Trabalho-Redes/blob/master/protocolo_Comunica%C3%A7%C3%A3o_Equipamento.pdf) é apresentado o protocolo comunicação entre os equipamentos.

Nossa aplicação foi implementada na linguagem Java 8 com a IDE Eclipse no sistema operacional Windows 10. Seu funcionamento ocorre de acordo com o protocolo estabelecido na primeira etapa do trabalho.

# Detalhes importantes de implementação em java
O gerenciador se comporta como servidor e os demais equipamentos como cliente. Para que o gerenciador fizesse a leitura simultânea dos diferentes equipamentos foi necessário utilizar uma comunicação não bloqueante, sendo utilizado as bibliotecas ServerSocketChannel e SocketChannel. 

# Execução
Os processos de cada dispositivo podem ser executados buscando o diretório /Simulador no diretório principal da aplicação pelo Prompt de Comando do Windows e inserindo o comando: 
 
java -jar <nome do executável>.jar 

Obs: O processo do Gerenciador deve ser o primeiro a ser executado. 

# Observações 
### Há 9 processos a serem executados simultâneamente: 
1. Cliente 
2. Gerenciador 
3. SensorTemperatura 
4. SensorCO2 
5. SensorUmidade 
6. Aquecedor 
7. Resfriador 
8. Injetor 
9. Irrigador  

### Mensagens de leitura
As mensagens de leitura enviadas pelos sensores ao gerenciador não são explicitadas no gerenciador por texto, visto que isso acarretaria em um spam,            impossibilitando a visualização das outras mensagens. Ainda é possível verificar essa troca através da mensagem 7 (Requisição da última             leitura dos sensores) do cliente para o gerenciador, notando-se que os valores são alterados e correspondem aos valores impressos nos processos dos sensores. 

### Simular temperatura ambiente
Para simular a contribuição da temperatura do ambiente na temperatura da estufa, o usuário pode abrir o arquivo “temperaturaAmbiente.txt” e alterar seu valor    manualmente. De acordo com a temperatura ambiente, a temperatura lida no sensor de temperatura sobe ou desce automaticamente. Assim, é possível testar o funcionamento tanto do Aquecedor quanto do Resfriador. 
