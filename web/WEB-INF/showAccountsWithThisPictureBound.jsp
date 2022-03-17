<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page contentType="text/html" pageEncoding="UTF-8"%>

    <div class="container d-flex justify-content-center">
        <c:forEach var="accountBox" items="${listAccountsWithThisPictureBound}">
            <div class="card shadow-sm m-1"  style="width: 10rem;">
                <img src="insertFile/${accountBox.picture.pathToFile}" style="height: 12rem;">
                <div class="card-body">
                    <p class="card-text"><a href="showAccount?accountId=${accountBox.id}">${accountBox.name}</a></p>
                    <p class="card-text"><a href="removeAccount?id=${accountBox.id}">Удалить</a></p>
              </div>
            </div>
        </c:forEach>
    </div>
