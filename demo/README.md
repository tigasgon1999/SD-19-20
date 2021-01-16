# Guião de Demonstração da Primeira Parte (tag SD_P1)

## 1. Preparação do Sistema

Para testar a aplicação e todos os seus componentes, é necessário preparar um ambiente com dados para proceder à verificação dos testes.

### 1.1. Compilar o Projeto

Primeiramente, é necessário instalar as dependências necessárias para o *silo* e os clientes (*eye* e *spotter*) e compilar estes componentes.
Para isso, basta ir à diretoria *root* do projeto e correr o seguinte comando:

```
$ mvn clean install -DskipTests
```

Com este comando já é possível analisar se o projeto compila na íntegra.

### 1.2. *Silo*

Para proceder aos testes, é preciso o servidor *silo* estar a correr. 
Para isso basta ir à diretoria *silo-server* e executar:

```
$ mvn exec:java
```

Este comando vai colocar o *silo* no endereço *localhost* e na porta *8080*.

### 1.3. *Eye*

Vamos registar 3 câmeras e as respetivas observações. 
Cada câmera vai ter o seu ficheiro de entrada próprio com observações já definidas.
Para isso basta ir à diretoria *eye* e correr os seguintes comandos:

```
$ ./eye localhost 8080 Tagus 38.737613 -9.303164 < eye1.txt
$ ./eye localhost 8080 Alameda 30.303164 -10.737613 < eye2.txt
$ ./eye localhost 8080 Lisboa 32.737613 -15.303164 < eye3.txt
```
**Nota:** Para correr o script *eye* é necessário fazer `mvn install` e adicionar ao *PATH* ou utilizar diretamente os executáveis gerados na diretoria `target/appassembler/bin/`.

Depois de executar os comandos acima já temos o que é necessário para testar o sistema. 

## 2. Teste das Operações

Nesta secção vamos correr os comandos necessários para testar todas as operações. 
Cada subsecção é respetiva a cada operação presente no *silo*.

### 2.1. *cam_join*

Esta operação já foi testada na preparação do ambiente, no entanto ainda é necessário testar algumas restrições.

#### 2.1.1. Teste das câmeras com nome duplicado e coordenadas diferentes.  
O servidor deve rejeitar esta operação. 
Para isso basta executar um *eye* com o seguinte comando:

```
$ ./eye localhost 8080 Tagus 10.0 10.0
```

#### 2.1.2. Teste do tamanho do nome.  
O servidor deve rejeitar esta operação. 
Para isso basta executar um *eye* com o seguinte comando:

```
$ ./eye localhost 8080 ab 10.0 10.0
$ ./eye localhost 8080 abcdefghijklmnop 10.0 10.0
```


### 2.2. *report*

Esta operação já foi testada acima na preparação do ambiente.

No entanto falta testar o sucesso do comando *zzz*. 
Na preparação foi adicionada informação que permite testar este comando.
Para testar basta abrir um cliente *spotter* e correr o comando seguinte:

```
> trail car 00AA00
```

O resultado desta operação deve ser duas observações pela câmera *Tagus* com intervalo de mais ou menos 5 segundos.

### 2.3. *track*

Esta operação vai ser testada utilizando o comando *spot* com um identificador.

#### 2.3.1. Teste com uma pessoa (deve devolver vazio):

```
> spot person 14388236
```

#### 2.3.2. Teste com uma pessoa:

```
> spot person 123456789
person,123456789,<timestamp>,Alameda,30.303164,-10.737613
```

#### 2.3.3. Teste com um carro:

```
> spot car 20SD21
car,20SD21,<timestamp>,Alameda,30.303164,-10.737613
```

### 2.4. *trackMatch*

Esta operação vai ser testada utilizando o comando *spot* com um fragmento de identificador.

#### 2.4.1. Teste com uma pessoa (deve devolver vazio):

```
> spot person 143882*
```

#### 2.4.2. Testes com uma pessoa:

```
> spot person 111*
person,111111000,<timestamp>,Tagus,38.737613,-9.303164

> spot person *000
person,111111000,<timestamp>,Tagus,38.737613,-9.303164

> spot person 111*000
person,111111000,<timestamp>,Tagus,38.737613,-9.303164
```

#### 2.4.3. Testes com duas ou mais pessoas:

```
> spot person 123*
person,123111789,<timestamp>,Alameda,30.303164,-10.737613
person,123222789,<timestamp>,Alameda,30.303164,-10.737613
person,123456789,<timestamp>,Alameda,30.303164,-10.737613

> spot person *789
person,123111789,<timestamp>,Alameda,30.303164,-10.737613
person,123222789,<timestamp>,Alameda,30.303164,-10.737613
person,123456789,<timestamp>,Alameda,30.303164,-10.737613

> spot person 123*789
person,123111789,<timestamp>,Alameda,30.303164,-10.737613
person,123222789,<timestamp>,Alameda,30.303164,-10.737613
person,123456789,<timestamp>,Alameda,30.303164,-10.737613
```

#### 2.4.4. Testes com um carro:

```
> spot car 00A*
car,00AA00,<timestamp>,Tagus,38.737613,-9.303164

> spot car *A00
car,00AA00,<timestamp>,Tagus,38.737613,-9.303164

> spot car 00*00
car,00AA00,<timestamp>,Tagus,38.737613,-9.303164
```

#### 2.4.5. Testes com dois ou mais carros:

```
> spot car 20SD*
car,20SD20,<timestamp>,Alameda,30.303164,-10.737613
car,20SD21,<timestamp>,Alameda,30.303164,-10.737613
car,20SD22,<timestamp>,Alameda,30.303164,-10.737613

> spot car *XY20
car,66XY20,<timestamp>,Lisboa,32.737613,-15.303164
car,67XY20,<timestamp>,Alameda,30.303164,-10.737613
car,68XY20,<timestamp>,Tagus,38.737613,-9.303164

> spot car 19SD*9
car,19SD19,<timestamp>,Lisboa,32.737613,-15.303164
car,19SD29,<timestamp>,Lisboa,32.737613,-15.303164
car,19SD39,<timestamp>,Lisboa,32.737613,-15.303164
car,19SD49,<timestamp>,Lisboa,32.737613,-15.303164
car,19SD59,<timestamp>,Lisboa,32.737613,-15.303164
car,19SD69,<timestamp>,Lisboa,32.737613,-15.303164
car,19SD79,<timestamp>,Lisboa,32.737613,-15.303164
car,19SD89,<timestamp>,Lisboa,32.737613,-15.303164
car,19SD99,<timestamp>,Lisboa,32.737613,-15.303164
```

### 2.5. *trace*

Esta operação vai ser testada utilizando o comando *trail* com um identificador.

#### 2.5.1. Teste com uma pessoa (deve devolver vazio):

```
> trail person 14388236
```

#### 2.5.2. Teste com uma pessoa:

```
> trail person 123456789
person,123456789,<timestamp>,Alameda,30.303164,-10.737613
person,123456789,<timestamp>,Alameda,30.303164,-10.737613
person,123456789,<timestamp>,Tagus,38.737613,-9.303164

```

#### 2.5.3. Teste com um carro (deve devolver vazio):

```
> trail car 12XD34
```

#### 2.5.4. Teste com um carro:

```
> trail car 00AA00
car,00AA00,<timestamp>,Tagus,38.737613,-9.303164
car,00AA00,<timestamp>,Tagus,38.737613,-9.303164
```

## 3. Considerações Finais

Para além de tudo o que foi descrito acima, outras situações estão cobertas nos testes de integração, especialmente casos de erro e pedidos mal formados.

No que toca ao *spotter*, há ainda o comando *help*, que lista e explica todas as operações disponíveis ao utilizador.
A usagem é a seguinte:

```
>>help
SpotterApp: Usage 
help                            Displays this menu."
ping    <msg>                   Health check of server. Should respond with "Hello <msg>!"."
clear                           Clears all server state."
init    <args>                  Sends initial control information to server."
spot    <type> <identifier>     Returns information on all objects of <type> that match <identifier>."
trail   <type> <identifier>     Returns complete information on object of <type> that exactly matches <identifier>."
```
---
# Guião de Demonstração da Segunda Parte (tag SD_P2)

## 1. Preparação do Sistema

Para testar a aplicação e todos os seus componentes, é necessário preparar um ambiente com dados para proceder à verificação dos testes.
Os argumentos entre [] são opcionais.

### 1.1. Compilar o Projeto

Primeiramente, é necessário instalar as dependências necessárias para o *silo* e os clientes (*eye* e *spotter*) e compilar estes componentes.
Para isso, basta ir à diretoria *root* do projeto e correr o seguinte comando:

```
$ mvn clean install -DskipTests
```

Com este comando já é possível analisar se o projeto compila na íntegra.

### 1.2. Correr ZooKeeper

Antes de podermos iniciar as réplicas e os clientes, devemos estar a correr o gestor de nomes ZooKeeper.

### 1.3. Iniciar uma réplica *Silo*
A estrutura geral para iniciar uma réplica é:
```
$ ./silo-server zooHost zooPort instance host port nReplicas [gossipTime]
```

### 1.4. Iniciar um cliente *Eye*
A estrutura geral para iniciar um cliente Eye é:
```
$ ./eye zooHost zooPort cameraName cameraLatitude cameraLongitude nReplicas [instance]
```

### 1.5. Iniciar um cliente *Spotter*
A estrutura geral para iniciar um cliente Spotter é:
```
$ ./spotter zooHost zooPort nReplicas [instance] 
```

## 2. Cenários de Demonstração da Replicação e Tolerância a Faltas

Para demonstrar corretamente o funcionamento da nossa solução, necessitamos de 2 *setups* diferentes.
O primeiro usa clientes fixos a uma dada réplica. Serve isto para demonstrar o protocolo *gossip* em ação, e como um cliente está protegido a mudanças de endereço no lado das réplicas.
O segundo *setup* usa clientes sem restrições de réplicas. Desta maneira, conseguimos demonstrar corretamente os mecanismos de tolerância a faltas e a leituras incoerentes do lado do cliente.

### 2.1. Parte 1 

O primeiro *setup* irá recorrer a duas réplicas, **R0** e **R1**, que irão comunicar entre si. Usaremos um intervalo de *gossip* curto (5s).
```
$ ./silo-server localhost zooPort 0 localhost 8080 2 5
$ ./silo-server localhost zooPort 1 localhost 8081 2 5
```
   * Nota: estes comandos devem ser efetuados em linhas de comando diferentes.

Para introduzir dados, iremos recorrer a um *eye* fixo a **R1** que irá introduzir, para além do *cam_join*, 2 updates, **U0** e **U1**. 
Noutra linha de comandos, efetuamos a seguinte sequência:
```
$ ./eye localhost zooPort CAMERA1 10 10 2 1
>>person,0      /*U0*/
>>person,1      /*U1*/
>><enter>
>>quit
Goodbye!
```
Finalmente, teremos um cliente *spotter* ligado fixamente a **R0**. Novamente numa linha de comandos nova:
```
$ ./spotter localhost zooPort 2 0
```

#### 2.1.1 Propagação de updates entre réplicas

Para verificar que o mecanismo *gossip* está a funcionar, basta efetuar uma leitura **L0** a **R0**. Esta leitura deverá refletir **U0**, que embora tenha sido reportado a **R1**, já está presente em **R0**.
```
<spotter>
>>spot person 0
person,0,<timestamp>,CAMERA1,10,10
```

#### 2.1.2. Recuperação de dados após falha transiente e mudança de endereço da réplica durante a execução

Estas duas funcionalidades podem ser testadas simultaneamente. Para isso basta desligar a réplica **R0**, pressionando "enter" na linha de comandos correspondente à mesma, e relançá-la num endereço diferente. 
```
$ ./silo-server localhost zooPort 0 localhost 8090 2 5
```

De seguida, **C1** efetua **L1** a **R0** (que entretanto foi desligada, trocada de endereço e relançada), que deverá refletir corretamente **U1**. 
```
<spotter>
>>spot person 1
person,1,<timestamp>,CAMERA1,10,10

```

Isto acontece porque um cliente fixo, em caso de falha, volta a pedir ao serviço de nomes o endereço do servido a que está fixo e porque o protocolo de *gossip* consegue corretamente re-atualizar uma réplica que tenha falhado. Podemos consultar o *stdout* de réplica **R0** para verificar que, 5 segundos após ter sido lançada, recebeu todos o dados que tinha anteriormente de **R1**.

#### 2.1.3. Final

Nesta secção demonstramos os mecanismos de propagação de updates, de recuperação de dados e de tolerância a *updates* no serviço de nomes.
Iremos agora preparar um novo setup, sendo necessário desligar o anterior. Para isto basta pressionar "enter" nas linhas de comando das réplicas e, naa linha de comando relativa ao cliente *spotter*, introduzir o comando `>>quit`. 

### 2.2. Parte 2

Nesta parte iremos demonstar mais alguns mecanismos do cliente, nomeadamente o mecanismo de tolerância a faltas e partições e o mecanismo que minimiza leituras incoerentes por parte do mesmo cliente.
O *setup* necessário é o seguinte:
Iremos necessitar, novamente, de duas réplicas, **R0** e **R1**, mas desta vez **não haverá _gossip_ entre elas**.

```
$ ./silo-server localhost zooPort 0 localhost 8080 2 -1
$ ./silo-server localhost zooPort 1 localhost 8081 2 -1
```
   * Nota: estes comandos devem ser efetuados em linhas de comando diferentes.

De seguida, iremos necessitar de um cliente *eye*, que irá reportar **U0** e **U1** a **R0** e **R1** respetivamente.
```
$ ./eye localhost zooPort CAMERA1 10 10 2 0
>>person,0
>><enter>
>>quit
Goodbye!
$ ./eye localhost zooPort CAMERA1 10 10 2 1
>>person,1
>><enter>
>>quit
Goodbye!
```

Note-se que, como não há *gossip*, **R0** não terá conhecimento de **U1** e **R1** não terá conhecimento de **U0**.
Posteriormente, lançamos um cliente *spotter* sem restrição de réplica. Este cliente irá ligar-se a uma das duas réplicas anteriores, que designaremos por **Rx**. Consequentemente, o cliente deve efetuar a leitura **Lx** que irá refletir **Ux**.
```
$ ./spotter localhost zooPort 2
>>spot person <x>
person,<x>,<timestamp>,CAMERA1,10,10
```
   * Nota: Para saber a que réplica está ligado o cliente, basta ir ao ficheiro *<data_hora_atual>*.log, presente na mesma diretoria onde foi corrido o *spotter*. Lá haverá um *log* de informação a explicitar a réplica a que está ligado.

Finalmente, desligamos **Rx**, pressionando "enter" na linha de comandos correspondente.

#### 2.2.1. Reconexão a outra réplica após falha

Neste teste demonstramos que um cliente sem restrições liga-se livremente a outra reṕlica em caso de falha da réplica a que estava conectado anteriormente. Para demonstar isto, basta o cliente efetuar a leitura **Ly**, que irá refletir **Uy**. 
```
<spotter>
>>spot person <y>
person,<y>,<timestamp>,CAMERA1,10,10
```

Isto significa que o cliente, que estava originalmente ligado a **Rx**, está agora forçosamente ligado a **Ry**, pois **Ry** é a única réplica disponível e também a única réplica que conhece **Uy**. 

É de notar que, em caso de falha de ligação com a réplica, o cliente tenta 3 vezes recontactar a réplica, decidindo que falhou se atingir um timeout. Ao fim de 3 tentativas, o cliente assume que a réplica está incontactável e o procedimento é idêntico ao detalhado anteriormente.

#### 2.2.2. Leituras coerentes com recurso a cache

Aqui é importante relembrar que **Rx** apenas conhece **Ux** e **Ry** apenas conhece **Uy**. De momento, o cliente encontra-se ligado a **Ry**, logo, se não existisse uma cache, uma leitura **Lx** não refletiria **Ux**, pois **Ux** nunca foi propagado. No entanto, efetuando esta leitura no nosso cliente, podemos ver que **Lx** reflete, de facto, **Ux**.
```
<spotter>
>>spot person <x>
person,<x>,<timestamp>,CAMERA1,10,10
```


#### 2.2.3. Final
Nesta secção foram desmonstrados com sucesso os mecanismos de tolerância a faltas do lado do cliente, bem como a nossa solução para a anomalia das leituras incoerentes. 
Finalmente, resta nos desligar a réplica restante, pressionando "enter" na linha de comandos correspondente, o cliente *spotter*, inserindo o comando `>>quit` na linha de comandos correspondente, e desligar o serviço de nomes ZooKeeper.
Assumimos também que as réplicas começam sempre no 0.
 
