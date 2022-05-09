# flowable-engine

Este repositório contém a engine do Flowable v. 6.7.2 com arquivos modificados para a utilização do MySQL como Database.



## Instalação

Para utilizar este repositório, você deve cloná-lo e acessar a pasta "docker". Dentro ela, execute o script "build-all-images.sh"

```bash
./build-all-images.sh
```

Isso irá criar as imagens docker localmente, a saber:

- flowable/flowable-ui: Interface gráfica do Flowable
- flowable/flowable-rest: REST API do Flowable
- flowable-ui-mysql: Container MySQL que será usado com o Flowable. Para utilizarmos uma Database separada, o docker-compose.yml deverá ser alterado.



Uma vez geradas as imagens, basta rodar o docker compose e o sistema do Flowable estará disponível:

```bash
docker-compose up -d
```



## Utilização

Para acessar o Flowable, basta acessar os seguintes endpoints:

http://localhost:8080/flowable-ui

http://localhost:8088/flowable-rest/service



## Licença
Apache 2.0
