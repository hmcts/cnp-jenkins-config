import hudson.model.User;
import jenkins.security.ApiTokenProperty;

// Get the actual token
u = User.get("admin")
tokprop =  u.getProperty(ApiTokenProperty.class)
actual_token = tokprop.getApiTokenInsecure()

return actual_token
