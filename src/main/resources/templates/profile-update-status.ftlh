<!DOCTYPE html>
<html lang="uk">
<head>
    <meta charset="UTF-8">
    <title>Зміна даних користувача</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            background-color: #f4f4f7;
            margin: 0;
            padding: 0;
        }

        .container {
            max-width: 600px;
            margin: 50px auto;
            background-color: #ffffff;
            border-radius: 8px;
            padding: 30px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        }

    </style>
</head>
<body>
<div class="container">
    <h1>Привіт, ${firstName}!</h1>
    <p>Ваша заявка на зміну даних профілю була розглянута.</p>
    <div>Статус: <strong>${status}</strong></div>
    <div>Дата: ${processedAt}</div>
    <#if rejectionReason?has_content>
        <p>Причина відмови: <strong>${rejectionReason}</strong></p>
    </#if>
</div>
</body>
</html>
