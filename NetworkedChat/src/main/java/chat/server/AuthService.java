package chat.server;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class AuthService {
    private final UserDAO userDAO = new UserDAO();

    public boolean register(String username, String password) {
        String hashed = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        return userDAO.addUser(username, hashed);
    }

    public boolean login(String username, String password) {
        String hashed = userDAO.getPassword(username);
        if (hashed == null) return false;
        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), hashed);
        return result.verified;
    }
}

