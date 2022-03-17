/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servlets;

import entity.AccountBox;
import entity.Picture;
import entity.User;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import session.AccountBoxFacade;
import session.PictureFacade;
import session.UserFacade;
import session.UserRolesFacade;
import tools.SymmetricCrypt;

/**
 *
 * @author Melnikov
 */
@WebServlet(name = "MyServlet",urlPatterns = {
    "/addAccountBox",
    "/createAccountBox",
    "/listAccounts",
    "/showAccount",
    "/removeAccount",
    "/showAccountsWithThisPictureBound",
        
})
public class MyServlet extends HttpServlet {
    @EJB AccountBoxFacade accountBoxFacade;
    @EJB PictureFacade pictureFacade;
    @EJB UserFacade userFacade;
    @EJB UserRolesFacade userRolesFacade;
    
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        if(session == null){
            request.setAttribute("info", "Авторизуйтесь");
            request.getRequestDispatcher("/showLogin").forward(request, response);
            return;
        }
        //Authentification
        User authUser = (User) session.getAttribute("authUser");
        if(authUser == null){
            request.setAttribute("info", "Авторизуйтесь");
            request.getRequestDispatcher("/showLogin").forward(request, response);
            return;
        }
        if(userRolesFacade.isRole("USER",authUser)){
            
        }
        String path = request.getServletPath();
        switch (path) {
            case "/addAccountBox":
                List<Picture> pictures = pictureFacade.findAllForUser(authUser);
                request.setAttribute("pictures", pictures);
                request.getRequestDispatcher("/WEB-INF/addAccountBox.jsp").forward(request, response);
                break;
            case "/createAccountBox":
                String name = request.getParameter("name");
                String picture = request.getParameter("picture");
                String url = request.getParameter("url");
                String urlLogin = request.getParameter("urlLogin");
                String urlPassword = request.getParameter("urlPassword");
                if(picture.isEmpty() || name.isEmpty() || url.isEmpty() || urlLogin.isEmpty() || urlPassword.isEmpty()){
                    request.setAttribute("info", "Заполните все поля");
                    request.setAttribute("name", name);
                    request.setAttribute("picture", picture);
                    request.setAttribute("url", url);
                    request.setAttribute("urlLogin", urlLogin);
                    request.setAttribute("urlPassword", urlPassword);
                    request.getRequestDispatcher("/addAccountBox").forward(request, response);
                    break;
                }
                Picture pic = null;
                try {
                    pic = pictureFacade.find(Long.parseLong(picture));
                    AccountBox accountBox = new AccountBox();
                    accountBox.setName(name);
                    accountBox.setPicture(pic);
                    accountBox.setUrl(url);
                    accountBox.setUrlLogin(urlLogin);
                    accountBox.setUrlPassword(urlPassword);
                    accountBoxFacade.create(accountBox);
                    authUser = userFacade.find(authUser.getId());
                    authUser.getListAccountBox().add(accountBox);
                    userFacade.edit(authUser);
                    session.setAttribute("authUser", authUser);
                    request.setAttribute("info", "Данные записаны успешно");
                    request.getRequestDispatcher("/addAccountBox").forward(request, response);
                } catch (Exception e) {
                    request.setAttribute("info", "Заполните все поля");
                    request.setAttribute("name", name);
                    request.setAttribute("picture", picture);
                    request.setAttribute("url", url);
                    request.setAttribute("urlLogin", urlLogin);
                    request.setAttribute("urlPassword", urlPassword);
                    request.getRequestDispatcher("/addAccountBox").forward(request, response);
                    break;
                }
                break;
            case "/listAccounts":
                authUser=(User) session.getAttribute("authUser");
                request.setAttribute("listAccounts", authUser.getListAccountBox());
                request.getRequestDispatcher("/WEB-INF/listAccounts.jsp").forward(request, response);
                break;
            case "/showAccount":
                String accountId = request.getParameter("accountId");
                if(accountId != null && accountId.isEmpty()){
                    request.setAttribute("info", "Неверный запрос");
                    request.getRequestDispatcher("/listAccounts").forward(request, response);
                    break;
                }
                try {
                    AccountBox ab = accountBoxFacade.find(Long.parseLong(accountId));
                    request.setAttribute("accountBox", ab);
                } catch (Exception e) {
                    request.setAttribute("info", "Неверный запрос");
                    request.getRequestDispatcher("/listAccounts").forward(request, response);
                    break;
                }
                request.getRequestDispatcher("/WEB-INF/showAccount.jsp").forward(request, response);
                break;
            case "/removeAccount":
                String id = request.getParameter("id");
                try {
                    for(AccountBox accountBox : authUser.getListAccountBox()){ //получаем аккаунты от вошедшего пользователя
                        if(accountBox.getId().equals(Long.parseLong(id))){ // выбираем указанный аккаунт из списка аккаунтов
                            authUser.getListAccountBox().remove(accountBox); // удаляем уакзанный аккаунт
                            userFacade.edit(authUser); // и сохраняем новое состояние пользователя в базу
                            session.setAttribute("authUser", authUser); //переписываем состояние обновленного вошедшего пользователя
                            int countAccountBox = accountBoxFacade.findAccountsWithThisPictureBond(accountBox.getPicture()).size();
                            if(countAccountBox > 1){ //если количество связанный с изображением аккаунтов больше 1, удаляем только аккаунты
                                accountBoxFacade.edit(accountBox); 
                                accountBoxFacade.remove(accountBox);
                                request.setAttribute("info", "Удален аккаунт: "+accountBox.getName());
                                break;
                            }
                            //если количество связанный с изображением аккаунтов больше 1 является ложью, 
                            //удаляем не только аккаунт, но и изображение из базы,
                            //но и с диска
                            accountBoxFacade.edit(accountBox); //ставим accountBox под управление контекста постоянства
                            accountBoxFacade.remove(accountBox); // удаляем управляемый объект
                            pic = pictureFacade.find(accountBox.getPicture().getId());
                            pictureFacade.remove(pic);// получаем и удаляем объект изображения из базы и далее с диска
                            File file = new File(pic.getPathToFile());
                            file.delete();
                            request.setAttribute("info", "Удален аккаунт: "+accountBox.getName());
                            break;
                        }
                    }
                    
                } catch (Exception e) {
                    request.setAttribute("info", "Удаление не удалось");
                }
                request.getRequestDispatcher("/listAccounts").forward(request, response);
                break;
            case "/showAccountsWithThisPictureBound":
                String pictureId = request.getParameter("pictureId");
                Picture pictureBoundWithAccounds = pictureFacade.find(Long.parseLong(pictureId));
                List<AccountBox> listAccountsWithThisPictureBound = accountBoxFacade.findAccountsWithThisPictureBond(pictureBoundWithAccounds);
                request.setAttribute("listAccountsWithThisPictureBound", listAccountsWithThisPictureBound);
                request.getRequestDispatcher("/WEB-INF/showAccountsWithThisPictureBound.jsp").forward(request, response);
                break;
        }
        
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
