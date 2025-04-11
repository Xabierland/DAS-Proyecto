# DAS-Proyecto

Proyecto de Desarrollo Avanzado de Software

## Integrantes

- [Xabier Gabiña Barañano](https://www.xabierland.com/)

## Descripción

La aplicacion se trata de una red social para compartir lecturas en la que puedes seguir llevar la cuenta de los libros que has leido y los que quieres leer, ademas de poder seguir a otros usuarios y ver sus lecturas.

## Tecnologías

- Java
- MySQL
- PHP

### Base de datos en MySQL

La base de datos se puede encontrar en el siguiente enlace:

[http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/phpmyadmin/](http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/phpmyadmin/)

El usuario es `Xxgabina001` y la contraseña es `BPKrbjdfP`

### API

Puedes ver la API de la aplicacion esta alojada en el siguiente enlace:

[http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/xgabina001/WEB/api.php](http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/xgabina001/WEB/api.php/)

Puedes acceder a la API mediante SFTP con el usuario `xgabina001` y la siguiente clave privada

```gpg
-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEAvJ3byRoOiURqq8Zdf2LeF3TiooaOXf58PMaWXrRyMp0Yws+X
v2v7K7hQulC6ZULehRQCcDZnXD/FMr0qrLY33P2nP9R5ggJ4gtTU6PXPVa/IU4ZH
eos1GBJkl1gdE//jRdroaxxt4Jr/QRyYYAVyCWLt5G27ZZ6E3koFfeszuzcV6SKE
DTyIOk06fWAWbURIjSOII6+upleyCZ9fi3dR1MKtyXTCqpRSQ6kwqcdT+4IaVYM2
Y+MI7cFSc/u8u/OiUC8m0A83vVFbRCg/NJJFZ/elZSyKCNKcZ2l4qZXa7j+XXbF/
PPtrFlKbsU+3U5f43FWuPeZHp+OTFcUS6YD0iwIDAQABAoIBACmqQVvKGr3t1BZ/
6jVylJbf5hR63sYaqj0OrTbca4GMvGSEUQQjCUfeHk8Z+CLYQ6loAyXgjrKND7Wm
nd+rHOn/SkvKVtEGVLKp6yJO8m7+NqVkuzNxvtfDcF8SQtIhK4ZJ1h+bFxaPhqVO
eMZ1TXyPUver2cKX4sfr2Ay5wSKwpcHzdJ/YebGX475WL/4iEqIvXNdhR6Ay4YxT
F41RKzyi7ULGSBZAUS8ti/7RChbSCdTQWntcVoqOwOImGzwhodRwrn7AFNrOyjFd
OXYdcOjAbYNm5yDZbxPKJxOxbPMve3ijYKkODRCy7Ia07QofhjFQzNdZEqLt8W6s
r9H/hCkCgYEA9aUPRb4d9AkhFF0qMZaT+acI2T1kOAIST9Wc+q4t8/HUML7su0KB
LiWemUj3hwZ06TwIWDJZiCAX8UQ9Z1nX0i58D3chHn7V9EhlmWM+IndFFaYXEBU0
eAf25Fm7JgopLtdbrtvKl4qDegpgIuWxYsW4UBW0w6RsbB732xMwGq0CgYEAxJFd
YQQR8YpkRP8pa+E1hSD0PUoYw/rJO/NiL6qBXBYRjlu5HrimarxCBnCnv/HiTGew
iAKyjDF15V7MeSt8zmE1au2nSMdNrGew/DrGgtnBo/8pGGpVPEbPj2GL3QnsBxaL
aBOMfnoAlmK0vAnx9/xI1tel2pcuSnqOpWhxqxcCgYEA6DI/ToCYlb4va61pHfil
JZ7TyW2zlxG2N4rHWVpeC7KzDeTbp2ME8xcSZPjJKfRbfMCHbr8hTIBxwVjs5g3R
6VaRZJkKUGY6XjA1AZoC+NdfEUivkj9JGpEEuvNq6Vk/doyRmKcgSMDg6PJ3z2Tp
mWEmiWULOsfoyQc46PepR/0CgYA8cJHsvaqiwTG5gVeEIzgomgxfOARLZjYv59L9
4whpfyOgyRUvnAkXcpE+l68MttwlMBC4kVPDBYZBo5dtfnCeqIcbPL2eBCIe67cg
pxXYsAn5WgCR0EgrA37YkF7H9UAMoeL1emVNkkfR9cGqu8gZvwnKfX4yqx+BqRNp
SqcAuwKBgQC5TjrD2dUA4JHVEGyg8M4RyDci9eyzomUx/DU1qHEr4ZwPbplQbUod
eerqLpcfeQICXkxr5cFPAr3cK3TKbSF8tCvAqszm1Js27MWP1XwMTKw6yFVC4XYK
1TDDfLEjxlmnWWpYeKYtjGsoiV6VDnoBR0Jfdb0+Ylv7M+6bhrWeOQ==
-----END RSA PRIVATE KEY-----
```

### Panel de mensajeria

La aplicacion cuenta con un panel de mensajeria que permite enviar mensajes a todas las instancias de la aplicacion. Puedes acceder a el mediante el siguiente enlace:

[http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/xgabina001/WEB/mensajeria.php](http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/xgabina001/WEB/admin_panel.php)

## TODO

- Alarma (Probablemente para actualizar la lista de recomendaciones del MainActivity)
- ContentProvider (Compartir libros) [Funciona más o menos ahora mismo]
- Servicio en primer plano (Contador de tiempo de lectura)