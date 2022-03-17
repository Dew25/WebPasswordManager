/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package session;

import entity.AccountBox;
import entity.Picture;
import entity.User;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Melnikov
 */
@Stateless
public class AccountBoxFacade extends AbstractFacade<AccountBox> {

    @PersistenceContext(unitName = "WebPasswordManagerPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public AccountBoxFacade() {
        super(AccountBox.class);
    }

    public List<AccountBox> findAccountsWithThisPictureBond(Picture pictureBoundWithAccounds) {
        try {
            return em.createQuery("SELECT ab FROM AccountBox ab WHERE ab.picture = :picture")
                    .setParameter("picture", pictureBoundWithAccounds)
                    .getResultList();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

   
    
}
