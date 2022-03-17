/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servlets;

import entity.Picture;
import entity.User;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import org.imgscalr.Scalr;
import session.PictureFacade;

/**
 * Выводит форму загрузки файлов со списком загруженных изображений, 
 * которые можно удалить.
 * 
 * @author jvm
 */
@WebServlet(name = "UploadServlet", urlPatterns = {
    "/uploadFile", 
    "/showUploadFile",
    "/deletePicture",
    
})
@MultipartConfig()
public class UploadServlet extends HttpServlet {
    @EJB private PictureFacade pictureFacade;
    
    
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
        // ----- защищаем кейсы от невошедших пользователей ----
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
        // ---- дальше пройдут только вошедшие пользователи ----
        String path = request.getServletPath();
        switch (path) {
            case "/showUploadFile":
                //Показываем форму добавления изображений со списком уже загруженных данным пользователем.
                List<Picture> picturs = pictureFacade.findAllForUser(authUser);
                request.setAttribute("picturs", picturs);
                request.getRequestDispatcher("/uploadFile.jsp").forward(request, response);
                break;
            case "/uploadFile":
                //---  Загружаем файл  ---
                //получаем список потоков (fileParts), отправленных клиентом в запросе на сохранение файлов
                List<Part> fileParts = request.getParts()
                .stream()//список преобразуем в поток
                .filter( part -> "file".equals(part.getName()))// отфильтровуем потоки от input-a c name="file" 
                .collect(Collectors.toList());//собираем полученные потоки (part) в список типа List<Part>
                String imagesFolder = "D:\\UploadDir\\WebPasswordManager\\"; // задаем имя папки для сохранения файлов. 
//                Properties prop = new Properties();
//                File imgDir = new File("/web/WEB-INF/pathToImages.properties");
//                prop.load(new FileReader(imgDir));
//                String imagesFolder = prop.getProperty("uploadDir");
                //Необходимо позаботиться о том, чтобы в эту директорию можно было писать (настройки OS)
                String imagesUserFolder =imagesFolder+authUser.getLogin(); //Формируем название папки для пользователя
                for(Part filePart : fileParts){// получаем из списка part с потоком байтов передаваемого от клиента файла
                    File dirForUserFiles = new File(imagesUserFolder); // создаем дескриптор файла (директории)
                    dirForUserFiles.mkdirs(); //создаем все директории, прописанные в этом дескрипторе
                    String pathToFile = imagesUserFolder+File.separatorChar
                                    +getFileName(filePart); //формируем путь к файлу полученному из функции getFileName
                    String pathToTempFile = imagesFolder+File.separatorChar+"tmp"+File.separatorChar+getFileName(filePart);
                    File tempFile = new File(pathToTempFile); //дескриптор к temp папке, куда будет загружаться большое изображение
                    tempFile.mkdirs(); //создаем все директории, прописанные в этом дескрипторе
                    try(InputStream fileContent = filePart.getInputStream()){ //получаем поток из part
                       Files.copy( // копируем байты из потока на жесткий диск по указанному адресу (URI)
                               fileContent,tempFile.toPath(), 
                               StandardCopyOption.REPLACE_EXISTING // если такой файл существует - переписываем
                       );
                       writeToFile(resize(tempFile),pathToFile);//записываем преобразованный по размеру файл и папки temp в папку пользователя
                       tempFile.delete(); //удаляем большой файл из папки temp
                    }
                    String description = request.getParameter("description");//считываем описание файла из запроса
                    Picture picture = new Picture(); // создаем объект изображения
                    picture.setDescription(description); // инициируем объект
                    picture.setPathToFile(pathToFile);
                    picture.setUser(authUser);
                    pictureFacade.create(picture); //записываем инициированный объект в базу
                }    
                request.setAttribute("info", "Файл успешно сохранен");
                request.getRequestDispatcher("/showUploadFile").forward(request, response);
                break;
            case "/deletePicture":
                String pictureId = request.getParameter("pictureId"); //получаем id удаляемого файла изображения
                Picture deletePicture = pictureFacade.find(Long.parseLong(pictureId)); //считываем объект из базы по id
                try {//защищаем следующий код 
                    pictureFacade.remove(deletePicture); //удаляем объект из базы
                    File deleteFile = new File(deletePicture.getPathToFile()); //дескриптор удаляемого файла
                    if(deleteFile.delete()){
                        request.setAttribute("info", "Файл успешно удален");
                    }else{
                        request.setAttribute("info", "Файл удалить не удалось");
                    };
                    request.getRequestDispatcher("/showUploadFile").forward(request, response);
                } catch (Exception e) { //если что то пошло не так отправляем на указанную страницу вызывая паттерн
                    request.setAttribute("info", "Изображение связано с аккаунтом!");
                    request.setAttribute("pictureId", deletePicture.getId());
                    request.getRequestDispatcher("/showAccountsWithThisPictureBound").forward(request, response);
                }
                break;
        }
    }
    private String getFileName(Part part){
        final String partHeader = part.getHeader("content-disposition");
        for (String content : part.getHeader("content-disposition").split(";")){
            if(content.trim().startsWith("filename")){
                return content
                        .substring(content.indexOf('=')+1)
                        .trim()
                        .replace("\"",""); 
            }
        }
        return null;
    }
    public void writeToFile(byte[] data, String fileName) throws IOException{
        try (FileOutputStream out = new FileOutputStream(fileName)) {
            out.write(data);
        }
    }
    public byte[] resize(File icon) {
        try {
           BufferedImage originalImage = ImageIO.read(icon);
           originalImage= Scalr.resize(originalImage, Scalr.Method.QUALITY, Scalr.Mode.FIT_TO_WIDTH,400);
            //To save with original ratio uncomment next line and comment the above.
            //originalImage= Scalr.resize(originalImage, 153, 128);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(originalImage, "jpg", baos);
            baos.flush();
            byte[] imageInByte = baos.toByteArray();
            baos.close();
            return imageInByte;
        } catch (Exception e) {
            return null;
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
