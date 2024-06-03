from bs4 import BeautifulSoup
from fake_useragent import UserAgent
from bs4 import BeautifulSoup
from selenium import webdriver
from selenium.webdriver.common.by import By
import sqlite3
import requests
import re
import time
import random
import markdown
import os
from tqdm import tqdm
#必要参数准备
# 获取当前日期,准备文件命名
current_date = time.strftime('%Y-%m-%d')

db_file ='spider/data_base/'+current_date+'_zhihu_hot_.db'
print("----------------------获取当前"+current_date+"热点文章------------------")
time.sleep(5)
# 创建UserAgent对象
# 伪装请求头，随机的User-Agent头部防止被禁用
ua = UserAgent()
user_agent = ua.random
headers = {
    "User-Agent": user_agent
}

# 定义数据库文件名称
def create_database():
    # 连接到SQLite数据库，如果不存在则创建
    # os.chdir("spider")
    folder_name = 'spider/data_base'
    if not os.path.exists(folder_name):
        os.makedirs(folder_name)
    # os.chdir("data_base")
    with sqlite3.connect(db_file) as conn:
    # 连接到临时数据库
        cursor = conn.cursor()
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS articles (
                id INTEGER PRIMARY KEY,
                title TEXT,
                url TEXT,
                excerpt TEXT
            )
        ''')        

# 获得知乎热榜       
def fetch_zhihu_hot(url):
    try:
        # 添加随机等待时间，模拟人类操作
        wait_time = random.uniform(2, 5)  # 随机等待2到5秒
        time.sleep(wait_time)

        response = requests.get(url, headers=headers)
        response.raise_for_status()  # 引发HTTP错误（如果有）
        data = response.json()
        return data
    except requests.exceptions.RequestException as e:
        print(f"Error fetching data: {e}")
        return None

# 拟人操作模拟人类浏览网页加载知乎热榜前100条回答
def Simulating_Humans(title,url,article_id):
    driver = webdriver.Firefox()  # 使用火狐浏览器
    driver.get(url)
    # 先获取页面资源用于初始设置
    page_source = driver.page_source

    # 用BeautifulSoup解析页面内容
    soup = BeautifulSoup(page_source, 'html.parser')

    # 提取 文章内容
    h4_tags = soup.find_all('h4', class_="List-headerText")
    num = [int(re.search(r'\d+', tag.get_text()).group()) for tag in h4_tags][0]
    max_comments = 50  # 设置评论数的最大值
    comment_count = 0  # 初始化评论数量
    temp = 0
    flag = 0
    output_flag = False
    print(f"Start")

    if num < max_comments:
        max_comments = num
    # 进度条设置
    progress_bar = tqdm(total=max_comments, desc="Loading Comments>>>", unit=" comments")
    # 加载评论
    while comment_count < max_comments:
        driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
        time.sleep(2)
        comment_count = len(driver.find_elements(By.CSS_SELECTOR, "div.List-item"))
        progress_bar.update(comment_count - temp)
        div_count = len(soup.find_all('button', text='写回答'))
        if div_count == 3:
            print("所有评论已加载.")
            break
        # 获取卡顿可能性推测
        if temp < max_comments and temp > max_comments / 2 and not output_flag:
            print("进度已过半，请耐心等待...")
            output_flag = True

        # #如果评论数较上次没有改变考虑是否是网络延迟问题或是评论加载完毕但评论数未到达最大值的情况
        if temp == comment_count:
          #设置flag来进行请情况的提醒
            flag += 1
        if flag == 1:
            print("有点卡顿...")
        elif flag == 2:
            print("可能是网络延迟...")
        elif flag == 3:
            print("请检查网络是否连接...")
            time.sleep(10)
        if flag == 4:
            print("事不过三我们刷新一下...")
            driver.refresh()
        if flag==5:
            print("建议等等再试")
        temp = comment_count  # 等待所有评论加载

    time.sleep(2)
    print("获取回答中..")
    page_source = driver.page_source
    driver.quit()  # 关闭浏览器

    # 使用BeautifulSoup解析页面
    soup = BeautifulSoup(page_source, 'html.parser')

    # 提取文章内容
    article_elements = soup.find_all('div', class_="RichContent RichContent--unescapable")
    user_links = soup.find_all('a', class_="UserLink-link")
    user_data_list = []
    for link in user_links:
        user_name = link.get_text()
        user_link = link['href']
        if(user_name!="" and user_link!=""):
            user_data_list.append((user_name, user_link))
    user_data_list=user_data_list[1:]
    # print(user_data_list)
    # 创建一个名为articles的文件夹，如果它不存在的话

    # os.chdir("..")
    folder_name = 'spider/articles'
    if not os.path.exists(folder_name):
        os.makedirs(folder_name)
    progress_bar = tqdm(total=max_comments, desc="saving articles", unit=" index")
    for index, article_element in enumerate(article_elements):
        article_content = article_element.get_text()
             # 获取对应的作者信息
        if index < len(user_data_list):
            author_name, author_link = user_data_list[index]
        else:
            author_name, author_link = "未爬取", "NONE"
            # 转换为Markdown格式
        markdown_text = f"作者: [{author_name}]({author_link})\n\n" +markdown.markdown(article_content)
    # 保存为Markdown文件
        filename = f"article_{index + 1}.md"
        file_path = os.path.join(folder_name, filename)
        with open(file_path, 'w', encoding='utf-8') as file:
            file.write(markdown_text)
        progress_bar.update(1)
    print(f"保存完毕,您可在article文件夹下查看哩wvw")
# 保存数据
def save_to_database(data):
    if data is not None:
        with sqlite3.connect(db_file) as conn:
            cursor = conn.cursor()
        cursor.execute("DELETE FROM articles")  # 清空临时数据库
        conn.commit()
        for article in data.get("data", []):
            target = article.get("target", {})
            title = target.get("title", "")
            url = target.get("url", "")
            modified_url = url.replace("api.", "www.")
            modified_url = modified_url.replace("questions", "question")
            excerpt = target.get("excerpt", "")
            cursor.execute("INSERT INTO articles (title, url, excerpt) VALUES (?, ?, ?)", (title, modified_url, excerpt))
        conn.commit()  # 提交更改
    else:
        print("我两眼空空")
def zhihu_hot():
    create_database()
    zhihu_url = "https://www.zhihu.com/api/v3/feed/topstory/hot-lists/total"
    zhihu_data = fetch_zhihu_hot(zhihu_url)
    # 如果获取成功    
    if zhihu_data:
        # 数据存储
        save_to_database(zhihu_data)
        #文章链接访问
        with sqlite3.connect(db_file) as conn:
            cursor = conn.cursor()
        cursor.execute("SELECT id,title FROM articles ORDER BY id  LIMIT 50")
        articles=cursor.fetchall()
        return articles
        # for item in article_title:
            # print(f"{item[0]}.  {item[1]}")
def get_answer(article_id):
    with sqlite3.connect(db_file) as conn:
            cursor = conn.cursor()
    # article_id=input("请输入您关注的问题id号(热榜第几):")
    print(article_id)
    cursor.execute("SELECT url FROM articles WHERE id = ?", (article_id,))
    url=cursor.fetchall()
    url=url[0][0]
    print(url)
    cursor.execute("SELECT title FROM articles WHERE id = ?", (article_id,))
    title=cursor.fetchall()
    title=title[0][0]
    print(title)
    Simulating_Humans(title,url,article_id)