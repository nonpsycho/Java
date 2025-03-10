# Foodlab 
Ваш незаменимый помощник в кулинарных эксперементах. Этот веб-сервис позволяет пользователям побуликовать, искать и делиться кулинарными рецептами. Вы можете добавлять свои рецепты, оставлять отзывы и сохранять понравившиеся рецепты.
## Содержание
- [Функциональность](#функциональность)
- [Технологии](#технологии)
- [Часто задаваемые вопросы](#часто-задаваемые-вопросы)
  
## Функцианальность
- Регистрация и аутентификация пользователей.
- Добавление, редактирование и удаление реецептов.
- Отставление отзывов, оценок к рецептам.
- Поиск и фильтрация рецептов по названию и ингридиентам.

## Технологии
- **Spring Boot** - для создания приложения.
- **Thymeleaf** - для создания пользовательского интерфейса.
- **Maven** - для управления зависимостями.

## Часто задаваемые вопросы
- **Как зарегистрироваться в системе?** Вы можете зарегистрироваться, отправив POST-запрос на /api/... с вашим именем пользователя и паролем.
- **Как добавить новый рецепт?** Чтобы добавить новый рецепт, отправьте POST-запрос на /api/... с необходимыми данными в теле запроса.
- **Могу ли я редактировать свои рецепты?** Да, вы можете редактировать свои рецепты, отправив PUT-запрос на /api/.../{...} с обновлёнными данными.
- **Как оставить отзыв на рецепт?** Вы можете оставить отзыв, отправив POST-запрос на /api/.../{...}/reviews с текстом вашего отзыва.
- **Как найти рецепт?** Вы можете получить список всех рецептов, отправив GET-запрос на /api/recipes и использовать параметры фильтрации для поиска.

# Ссылка на Sonar
https://sonarcloud.io/project/overview?id=nonpsycho_Java2
