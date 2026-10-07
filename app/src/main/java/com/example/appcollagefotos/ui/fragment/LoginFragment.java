package com.example.appcollagefotos.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.appcollagefotos.data.model.Event;
import com.example.appcollagefotos.data.model.User;
import com.example.appcollagefotos.data.repository.FirebaseRepository;
import com.example.appcollagefotos.databinding.FragmentLoginBinding;

public class LoginFragment extends Fragment {

    private static final String ARG_EVENT = "arg_event";

    public interface LoginListener {
        void onUserRegistered(Event event, User user);
    }

    private FragmentLoginBinding binding;
    private Event event;
    private LoginListener listener;
    private final FirebaseRepository repository = new FirebaseRepository();

    public static LoginFragment newInstance(Event event) {
        LoginFragment fragment = new LoginFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_EVENT, event);
        fragment.setArguments(args);
        return fragment;
    }

    public void setListener(LoginListener listener) {
        this.listener = listener;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof LoginListener) {
            this.listener = (LoginListener) context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            event = (Event) getArguments().getSerializable(ARG_EVENT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnRegister.setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {
        if (binding == null) return;

        String phoneNumber = binding.etPhoneNumber.getText() != null ?
                binding.etPhoneNumber.getText().toString().trim() : "";
        String email = binding.etEmail.getText() != null ?
                binding.etEmail.getText().toString().trim() : "";

        if (TextUtils.isEmpty(phoneNumber) || phoneNumber.length() < 7) {
            showError("Por favor ingresa un número de teléfono válido.");
            return;
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Por favor ingresa un correo electrónico válido.");
            return;
        }

        setLoading(true);

        repository.findUserByPhoneNumber(phoneNumber, new FirebaseRepository.SimpleCallback<User>() {
            @Override
            public void onSuccess(User existingUser) {
                if (existingUser != null) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            setLoading(false);
                            if (listener != null) {
                                listener.onUserRegistered(event, existingUser);
                            }
                        });
                    }
                } else {
                    createNewUserAccount(phoneNumber, email);
                }
            }

            @Override
            public void onError(Exception e) {
                createNewUserAccount(phoneNumber, email);
            }
        });
    }

    private void createNewUserAccount(String phoneNumber, String email) {
        repository.signInAnonymouslyIfNeeded(new FirebaseRepository.SimpleCallback<String>() {
            @Override
            public void onSuccess(String userId) {
                repository.saveUserProfile(userId, phoneNumber, email, new FirebaseRepository.SimpleCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                setLoading(false);
                                User newUser = new User(userId, phoneNumber, email, null);
                                if (listener != null) {
                                    listener.onUserRegistered(event, newUser);
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                setLoading(false);
                                showError("Error al guardar perfil: " + e.getLocalizedMessage());
                            });
                        }
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        setLoading(false);
                        showError("Error de autenticación: " + e.getLocalizedMessage());
                    });
                }
            }
        });
    }

    private void setLoading(boolean isLoading) {
        if (binding != null) {
            binding.btnRegister.setEnabled(!isLoading);
            binding.progressBarLogin.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.tvLoginError.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        if (binding != null) {
            binding.tvLoginError.setText(message);
            binding.tvLoginError.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
