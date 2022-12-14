package tg_bot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tg_bot.model.NotificationTask;
import tg_bot.repository.NotificationTaskRepository;


import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private final NotificationTaskRepository notificationTaskRepository;

    private final Pattern pattern = Pattern.compile("([0-9.:\\s]{16})(\\s)([\\W+]+)");

    @Autowired
    private TelegramBot telegramBot;

    public TelegramBotUpdatesListener(NotificationTaskRepository notificationTaskRepository) {
        this.notificationTaskRepository = notificationTaskRepository;

    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        for (Update update : updates) {
            logger.info("Processing update: {}", update);

            var chatId = update.message().chat().id();
            var text = update.message().text();
            var messageText = "Привет, " + update.message().chat().firstName();
            Matcher matcher = pattern.matcher(update.message().text());


            if (update.message() != null && "/start".equals(text)) {
                telegramBot.execute(new SendMessage(chatId, messageText));
            } else if (update.message() != null && matcher.matches()) {
                //обработка ситуации, когда строка соответствует паттерну
                save(create(chatId, matcher.group(3), matcher.group(1)));
            } else {
                telegramBot.execute(new SendMessage(chatId,
                        "Отправьте сообщение в виде: 01.01.2022 20:00 Сделать домашнюю работу"));
            }
            // Process your updates here
        }
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    //сохраняем данные в БД
    public void save(NotificationTask notificationTask) {
        logger.info("Save data");
        notificationTaskRepository.save(notificationTask);
    }

    public NotificationTask create(Long chatId, String message, String date) {
        logger.info("Create");
        NotificationTask notificationTask = new NotificationTask();
        notificationTask.setChatId(chatId);
        notificationTask.setMessage(message);
        notificationTask.setDate(LocalDateTime.
                parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        return notificationTask;
    }

    @Scheduled(cron = "0 0/1 * * * *")
    public void findByDate(){
        logger.info("find by date");
        List<NotificationTask> notificationTasks = notificationTaskRepository.
                findByDate(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
        notificationTasks.forEach(user ->telegramBot.execute(new SendMessage(user.getChatId(),user.getMessage())));
    }
}
