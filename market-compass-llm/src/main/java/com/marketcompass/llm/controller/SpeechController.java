package com.marketcompass.llm.controller;

import com.marketcompass.llm.dto.SetupRequest;
import com.marketcompass.llm.dto.TranscribeResponse;
import com.marketcompass.llm.groq.GroqSpeechService;
import com.marketcompass.llm.groq.GroqLLMService;
import com.marketcompass.llm.service.SpeechService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;