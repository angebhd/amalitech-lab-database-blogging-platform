package amalitech.blog.service;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

class PasswordHashService {
  private static final int TIME_COST = 3;
  private static final int MEMORY_COST = 131072; // 128 MB (in KB)
  private static final int PARALLELISM = 2;

  private final Argon2 argon2;

  public PasswordHashService() {
    this.argon2 = Argon2Factory.create(
            Argon2Factory.Argon2Types.ARGON2id
    );
  }

  public String hash(char[] password) {
    try {
      return argon2.hash(
              TIME_COST,
              MEMORY_COST,
              PARALLELISM,
              password
      );
    } finally {
      argon2.wipeArray(password);
    }
  }

  public boolean verify(char[] password, String hash) {
    try {
      return argon2.verify(hash, password);
    } finally {
      argon2.wipeArray(password);
    }
  }
}
